package twobiers.keycloak.resource;

public record ImpersonationResponseDto(
		boolean sameRealm,
		String redirect) {

}
