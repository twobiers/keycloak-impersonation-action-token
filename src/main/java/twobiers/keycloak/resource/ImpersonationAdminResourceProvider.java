package twobiers.keycloak.resource;

import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.services.resources.admin.AdminEventBuilder;
import org.keycloak.services.resources.admin.ext.AdminRealmResourceProvider;
import org.keycloak.services.resources.admin.fgap.AdminPermissionEvaluator;

public class ImpersonationAdminResourceProvider implements AdminRealmResourceProvider {
	public ImpersonationAdminResourceProvider() {
	}

	@Override
	public Object getResource(KeycloakSession session, RealmModel realm, AdminPermissionEvaluator auth,
			AdminEventBuilder adminEvent) {
		return new ImpersonationAdminResource(session, realm, auth);
	}

	@Override
	public void close() {

	}
}
