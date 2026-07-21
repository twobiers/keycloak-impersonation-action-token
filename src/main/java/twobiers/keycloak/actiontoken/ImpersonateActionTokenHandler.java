package twobiers.keycloak.actiontoken;

import java.net.URI;

import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.Response.Status;

import org.keycloak.TokenVerifier;
import org.keycloak.authentication.actiontoken.AbstractActionTokenHandler;
import org.keycloak.authentication.actiontoken.ActionTokenContext;
import org.keycloak.authentication.actiontoken.TokenUtils;
import org.keycloak.common.ClientConnection;
import org.keycloak.common.util.Time;
import org.keycloak.events.Details;
import org.keycloak.events.Errors;
import org.keycloak.events.EventBuilder;
import org.keycloak.events.EventType;
import org.keycloak.models.DefaultActionTokenKey;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.SingleUseObjectKeyModel;
import org.keycloak.models.SingleUseObjectProvider;
import org.keycloak.models.UserModel;
import org.keycloak.models.UserSessionModel;
import org.keycloak.services.ErrorPage;
import org.keycloak.services.managers.AuthenticationManager;
import org.keycloak.services.managers.AuthenticationSessionManager;
import org.keycloak.services.managers.UserSessionManager;
import org.keycloak.services.messages.Messages;

import static org.keycloak.models.ImpersonationSessionNote.IMPERSONATOR_ID;
import static org.keycloak.models.ImpersonationSessionNote.IMPERSONATOR_USERNAME;

public class ImpersonateActionTokenHandler extends AbstractActionTokenHandler<ImpersonateActionToken> {

	public ImpersonateActionTokenHandler() {
		super(ImpersonateActionToken.TOKEN_TYPE, ImpersonateActionToken.class, Messages.EXPIRED_ACTION,
				EventType.IMPERSONATE, Errors.INVALID_TOKEN);
	}

	@Override
	public TokenVerifier.Predicate<? super ImpersonateActionToken>[] getVerifiers(
			ActionTokenContext<ImpersonateActionToken> tokenContext) {
		return TokenUtils.predicates();
	}

	@Override
	public Response handleToken(ImpersonateActionToken token, ActionTokenContext<ImpersonateActionToken> tokenContext) {
		KeycloakSession session = tokenContext.getSession();
		RealmModel realm = tokenContext.getRealm();
		UserModel user = session.users().getUserById(realm, token.getUserId());
		ClientConnection clientConnection = tokenContext.getClientConnection();
		EventBuilder event = new EventBuilder(realm, session, clientConnection);

		// Normally, action tokens are invalidated after the intended required action is
		// executed. However, since
		// impersonation doesn't have a required action and is instead executed
		// immediately when the token is handled,
		// we need to invalidate the token here to prevent it from being used multiple
		// times.
		if (!invalidateActionToken(session, token.serializeKey(), 0L)) {
			return handleImpersonationError(tokenContext, "Action token already used", Status.BAD_REQUEST);
		}

		if (user == null) {
			return handleImpersonationError(tokenContext, "User not found", Status.NOT_FOUND);
		}
		if (!user.isEnabled()) {
			return handleImpersonationError(tokenContext, "User is disabled", Status.BAD_REQUEST);
		}
		if (user.getServiceAccountClientLink() != null) {
			return handleImpersonationError(tokenContext, "Service accounts cannot be impersonated",
					Status.BAD_REQUEST);
		}

		// If the current user is already impersonating another user, we expire the
		// existing session to prevent
		// multiple impersonations at the same time.
		UserSessionModel activeUserSession = session.getContext().getUserSession();
		if (activeUserSession != null && !activeUserSession.getUser().getId().equals(user.getId())) {
			AuthenticationManager.expireIdentityCookie(session);
			AuthenticationManager.expireRememberMeCookie(session);
			AuthenticationManager.expireAuthSessionCookie(session);
			AuthenticationManager.backchannelLogout(session, realm, activeUserSession, session.getContext().getUri(),
					clientConnection, session.getContext().getHttpRequest().getHttpHeaders(), true);
		}

		UserSessionModel userSession = new UserSessionManager(session).createUserSession(realm, user,
				user.getUsername(),
				clientConnection.getRemoteHost(), "impersonate", false, null, null);
		userSession.setNote(IMPERSONATOR_ID.toString(), token.getImpersonatorId());
		userSession.setNote(IMPERSONATOR_USERNAME.toString(), token.getImpersonatorUsername());

		AuthenticationManager.createLoginCookie(session, realm, userSession.getUser(), userSession,
				session.getContext().getUri(), clientConnection);
		URI redirect = URI.create(token.getRedirectUri());

		event.event(EventType.IMPERSONATE)
				.session(userSession)
				.user(user)
				.detail(Details.IMPERSONATOR_REALM, token.getImpersonatorRealm())
				.detail(Details.IMPERSONATOR, token.getImpersonatorUsername())
				.success();

		return Response.status(Response.Status.FOUND)
				.location(redirect)
				.build();
	}

	@Override
	public boolean canUseTokenRepeatedly(ImpersonateActionToken token,
			ActionTokenContext<ImpersonateActionToken> tokenContext) {
		return false;
	}

	private Response handleImpersonationError(ActionTokenContext<?> tokenContext, String errorMessage, Status status) {
		if (tokenContext != null && tokenContext.getAuthenticationSession() != null) {
			new AuthenticationSessionManager(tokenContext.getSession())
					.removeAuthenticationSession(tokenContext.getRealm(), tokenContext.getAuthenticationSession(),
							true);
		}

		return ErrorPage.error(tokenContext.getSession(), null, status, errorMessage);
	}

	private boolean invalidateActionToken(KeycloakSession session, String actionTokenKeyToInvalidate,
			long skewSeconds) {
		SingleUseObjectKeyModel actionTokenKey = DefaultActionTokenKey.from(actionTokenKeyToInvalidate);
		SingleUseObjectProvider singleUseObjectProvider = session.singleUseObjects();
		return singleUseObjectProvider.putIfAbsent(actionTokenKeyToInvalidate + SingleUseObjectProvider.REVOKED_KEY,
				actionTokenKey.getExp() - Time.currentTimeSeconds() + skewSeconds);
	}
}
