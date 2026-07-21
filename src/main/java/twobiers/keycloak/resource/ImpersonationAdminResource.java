package twobiers.keycloak.resource;

import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.Response.Status;
import twobiers.keycloak.actiontoken.ImpersonateActionToken;

import java.net.URI;

import org.keycloak.common.util.Time;
import org.keycloak.models.Constants;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserModel;
import org.keycloak.services.ErrorResponse;
import org.keycloak.services.Urls;
import org.keycloak.services.resources.LoginActionsService;
import org.keycloak.services.resources.admin.fgap.AdminPermissionEvaluator;
import org.keycloak.services.resources.admin.fgap.UserPermissionEvaluator;

public class ImpersonationAdminResource {
	private final KeycloakSession session;
	private final RealmModel realm;
	private final AdminPermissionEvaluator auth;

	public ImpersonationAdminResource(KeycloakSession session, RealmModel realm, AdminPermissionEvaluator auth) {
		this.session = session;
		this.realm = realm;
		this.auth = auth;
	}

	@POST
	@Path("users/{user-id}")
	@Consumes(MediaType.APPLICATION_JSON)
	@Produces(MediaType.APPLICATION_JSON)
	public Response impersonateUser(@PathParam("user-id") String userId) {
		UserModel user = session.users().getUserById(realm, userId);
		UserModel adminUser = auth.adminAuth().getUser();
		if (user == null) {
			return Response.status(Response.Status.NOT_FOUND).build();
		}

		final UserPermissionEvaluator userPermissionEvaluator = auth.users();
		userPermissionEvaluator.requireImpersonate(user);

		if (!user.isEnabled()) {
			throw ErrorResponse.error("User is disabled", Status.BAD_REQUEST);
		}
		if (user.getServiceAccountClientLink() != null) {
			throw ErrorResponse.error("Service accounts cannot be impersonated", Status.BAD_REQUEST);
		}

		URI redirect = Urls.accountBase(session.getContext().getUri().getBaseUri()).build(realm.getName());

		ImpersonateActionToken token = new ImpersonateActionToken(user.getId(), redirect.toString(),
				adminUser.getUsername(),
				adminUser.getId(),
				realm.getName(), (int) (Time.currentTimeSeconds() + 180));

		String impersonationLink = LoginActionsService.actionTokenProcessor(session.getContext().getUri())
				.queryParam(Constants.KEY, token.serialize(session, realm, session.getContext().getUri()))
				.build(realm.getName()).toString();

		ImpersonationResponseDto responseDto = new ImpersonationResponseDto(
				false,
				impersonationLink);

		return Response.ok().entity(responseDto).build();
	}
}
