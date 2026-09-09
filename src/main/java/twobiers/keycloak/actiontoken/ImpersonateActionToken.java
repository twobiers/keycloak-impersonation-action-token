package twobiers.keycloak.actiontoken;

import org.keycloak.authentication.actiontoken.DefaultActionToken;
import com.fasterxml.jackson.annotation.JsonProperty;

public class ImpersonateActionToken extends DefaultActionToken {

	public static final String TOKEN_TYPE = "impersonate";

	@JsonProperty("impersonator")
	private String impersonatorUsername;

	@JsonProperty("impersonatorId")
	private String impersonatorId;

	@JsonProperty("impersonatorRealm")
	private String impersonatorRealm;

	@JsonProperty("reduri")
	private String redirectUri;

	@JsonProperty("impersonatorSession")
	private String impersonatorSessionId;

	public ImpersonateActionToken(
			String userId,
			String redirectUri,
			String impersonatorId,
			String impersonatorUsername,
			String impersonatorRealm,
			int absoluteExpirationInSecs,
			String impersonatorSessionId) {
		super(userId, TOKEN_TYPE, absoluteExpirationInSecs, null);
		this.impersonatorUsername = impersonatorUsername;
		this.impersonatorId = impersonatorId;
		this.impersonatorRealm = impersonatorRealm;
		this.redirectUri = redirectUri;
		this.impersonatorSessionId = impersonatorSessionId;
	}

	private ImpersonateActionToken() {
	}

	public String getImpersonatorId() {
		return impersonatorId;
	}

	public void setImpersonatorId(String impersonatorId) {
		this.impersonatorId = impersonatorId;
	}

	public String getImpersonatorUsername() {
		return impersonatorUsername;
	}

	public void setImpersonatorUsername(String impersonatorUsername) {
		this.impersonatorUsername = impersonatorUsername;
	}

	public void setImpersonatorRealm(String impersonatorRealm) {
		this.impersonatorRealm = impersonatorRealm;
	}

	public String getImpersonatorRealm() {
		return impersonatorRealm;
	}

	public String getRedirectUri() {
		return redirectUri;
	}

	public void setRedirectUri(String redirectUri) {
		this.redirectUri = redirectUri;
	}

	/**
	 * @return the id of the administrator's own user session that should be terminated when this token is redeemed,
	 * or {@code null} when the administrator's session must not be touched (e.g. cross-realm impersonation).
	 */
	public String getImpersonatorSessionId() {
		return impersonatorSessionId;
	}

	public void setImpersonatorSessionId(String impersonatorSessionId) {
		this.impersonatorSessionId = impersonatorSessionId;
	}
}
