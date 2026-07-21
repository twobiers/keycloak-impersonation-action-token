package twobiers.keycloak.resource;

import org.keycloak.Config.Scope;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.KeycloakSessionFactory;
import org.keycloak.services.resources.admin.ext.AdminRealmResourceProvider;
import org.keycloak.services.resources.admin.ext.AdminRealmResourceProviderFactory;

public class ImpersonationAdminResourceProviderFactory implements AdminRealmResourceProviderFactory {
	public static final String PROVIDER_ID = "impersonation";

	@Override
	public AdminRealmResourceProvider create(KeycloakSession session) {
		return new ImpersonationAdminResourceProvider();
	}

	@Override
	public void init(Scope config) {

	}

	@Override
	public void postInit(KeycloakSessionFactory factory) {

	}

	@Override
	public void close() {

	}

	@Override
	public String getId() {
		return PROVIDER_ID;
	}
}
