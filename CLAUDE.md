# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## What This Is

A Keycloak SPI (Service Provider Interface) extension that implements a link-based impersonation flow
using action tokens, instead of Keycloak's default direct-impersonation-via-cookie approach. It solves
https://github.com/keycloak/keycloak/issues/10655 and is the subject of upstream PR
https://github.com/keycloak/keycloak/pull/40767.

The flow: an admin calls a custom admin REST endpoint to impersonate a user; instead of impersonating
immediately, the endpoint returns a short-lived signed link (an action token) that, when visited,
performs the actual impersonation. This makes impersonation an explicit, auditable, link-based action
rather than a synchronous cookie swap.

## Key Commands

```sh
# Build the SPI jar (runs tests, produces target/twobiers.keycloak-impersonation-action-token.jar via shade plugin)
./mvnw clean package

# Skip tests
./mvnw clean package -DskipTests

# Run tests only (Testcontainers-based, spins up a real Keycloak container — needs Docker)
./mvnw test

# Run a single test
./mvnw test -Dtest=CustomActionTokenTest#testCustomActionToken

# Run a local Keycloak with the built jar mounted, behind nginx for hostname-based admin routing
docker compose up
```

`docker compose up` requires the jar to already be built at
`target/twobiers.keycloak-impersonation-action-token.jar` (run `./mvnw clean package` first). It exposes
Keycloak at `http://keycloak.localhost:8080` and the admin console at `http://keycloak-admin.localhost:8080`
(add both hostnames to `/etc/hosts` pointing at `127.0.0.1`). Bootstrap admin credentials are `admin`/`admin`.

## Architecture

Two independent Keycloak SPI extension points are registered via `META-INF`-style provider files in
`src/main/resources/` (files named after the SPI interface, containing the implementation class name —
standard Java `ServiceLoader` convention that Keycloak's SPI loader also uses):

- `org.keycloak.authentication.actiontoken.ActionTokenHandlerFactory` → registers
  `ImpersonateActionTokenHandler`
- `org.keycloak.services.resources.admin.ext.AdminRealmResourceProviderFactory` → registers
  `ImpersonationAdminResourceProviderFactory`

**Admin REST resource** (`twobiers.keycloak.resource` package): a chain of
`ImpersonationAdminResourceProviderFactory` → `ImpersonationAdminResourceProvider` →
`ImpersonationAdminResource`, mirroring Keycloak's standard admin extension SPI pattern. The resource
exposes `POST /admin/realms/{realm}/impersonation/users/{user-id}`, checks the caller
has impersonate permission via `AdminPermissionEvaluator`/`UserPermissionEvaluator`, then builds a signed
`ImpersonateActionToken` (180s expiry) and returns it wrapped in an action-token processor URL
(`ImpersonationResponseDto`). Nothing is impersonated yet at this point.

**Action token handling** (`twobiers.keycloak.actiontoken` package): `ImpersonateActionToken` is a
`DefaultActionToken` subclass carrying the target user, impersonator id/username/realm, and a redirect
URI. `ImpersonateActionTokenHandler` (registered under `TOKEN_TYPE = "impersonate"`) is invoked when the
signed link is visited; it single-use-invalidates the token first (impersonation has no required-action
step, so it executes immediately rather than being deferred), validates the target user
(exists/enabled/not a service account), tears down any pre-existing impersonation session on the caller,
creates a new `UserSessionModel` for the impersonated user with `IMPERSONATOR_ID`/`IMPERSONATOR_USERNAME`
notes, sets the login cookie, fires an `EventType.IMPERSONATE` event, and redirects.

**`nginx.conf`** exists solely to rewrite the real-world admin-console path
(`/auth/admin/realms/{realm}/users/{id}/impersonation`) to this extension's custom path
(`/auth/admin/realms/{realm}/impersonation/users/{id}`) via server-name-based routing between
`keycloak.localhost` and `keycloak-admin.localhost`, simulating how a reverse proxy would splice this
extension into an existing Keycloak admin console UI without modifying the frontend.

## Dependencies of note

- Targets Keycloak `26.7.0` SPI APIs (`keycloak.version` in `pom.xml`); uses the
  `com.github.dasniko:keycloak-spi-bom` for dependency alignment.
- Java 21, built with the Maven Shade plugin into a single deployable jar dropped into
  `/opt/keycloak/providers/`.
- Tests use `testcontainers-keycloak` (dasniko) against `quay.io/keycloak/keycloak:nightly`, plus
  `rest-assured` for HTTP assertions — real integration tests against a live Keycloak, not mocks.
