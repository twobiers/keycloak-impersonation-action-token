package twobiers.keycloak;

import dasniko.testcontainers.keycloak.KeycloakContainer;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.keycloak.admin.client.Keycloak;
import org.keycloak.representations.idm.RealmRepresentation;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;
import java.util.Map;

import static io.restassured.RestAssured.given;

@Testcontainers
public class CustomActionTokenTest {

	@Container
	private static final KeycloakContainer keycloak = new KeycloakContainer("quay.io/keycloak/keycloak:nightly")
			.withDefaultProviderClasses();

	@BeforeAll
	static void beforeAll() {
		Keycloak admin = keycloak.getKeycloakAdminClient();
		RealmRepresentation realm = new RealmRepresentation();
		realm.setRealm("demo");
		realm.setEnabled(true);
		realm.setRegistrationEmailAsUsername(true);
		admin.realms().create(realm);
	}

	@Test
	public void testCustomActionToken() throws IOException {
		Keycloak admin = keycloak.getKeycloakAdminClient();
		String accessTokenString = admin.tokenManager().getAccessTokenString();

		// TODO: Implement a test that uses the custom action token and verifies its
		// behavior.
	}

}
