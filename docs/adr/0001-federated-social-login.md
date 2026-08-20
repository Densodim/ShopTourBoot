# ADR-0001: Federated Google / Apple login via ID tokens

**Date**: 2026-08-20
**Status**: accepted
**Deciders**: ShopTourr / ShopTourBoot

## Context

Voyage already issues first-party HS256 access/refresh JWTs after email/password. Product now needs Sign in with Google and Sign in with Apple. Browser Authorization Code + PKCE (Auth Tab / `ASWebAuthenticationSession`) is the right generic OAuth shape, but Google and Apple both expose native identity tokens that the API can verify without putting a client secret in the app.

## Decision

Clients obtain a Google or Apple **ID token** with official platform APIs, then `POST /api/auth/oauth` with `{ provider, idToken, nonce }`. ShopTourBoot verifies the JWT against the provider JWKS (Nimbus, already on the classpath), then issues the same first-party session tokens as password login.

- Android Google: Credential Manager `GetGoogleIdOption` (audience = Web client ID)
- iOS Apple: `ASAuthorizationAppleIDProvider`
- iOS Google: `ASWebAuthenticationSession` + PKCE, then Google token endpoint (public iOS client, no secret)
- Android Apple: `AuthTabIntent` to Apple's authorize endpoint (`response_type=code id_token`) using a Services ID

## Alternatives Considered

### Unofficial KMP OAuth library
- **Pros**: one common implementation
- **Cons**: auth is a trust boundary; no official KMP library matches Google/Apple review requirements
- **Why not**: rejected for this surface

### Browser PKCE for every provider
- **Pros**: one flow; matches generic OAuth blogs
- **Cons**: worse Google UX than Credential Manager; App Store expects native Sign in with Apple
- **Why not**: used only where a native ID token is unavailable (Apple on Android, Google on iOS)

### Spring Authorization Server / Google client secret in the app
- **Pros**: full OAuth server
- **Cons**: extra moving parts; secrets in the APK
- **Why not**: ID token verification is enough to mint our JWTs

## Consequences

### Positive
- No new Boot dependencies
- Password accounts can be linked when the IdP email is verified
- Apple-only users can exist without a password hash

### Negative
- Needs Google Cloud OAuth clients and Apple Services ID / Sign in with Apple capability
- Android Apple requires Chrome/Firefox Auth Tab

### Risks
- Apple email is often missing on later logins — lookup is by `apple_sub`
- Empty `VOYAGE_GOOGLE_CLIENT_IDS` / `VOYAGE_APPLE_AUDIENCES` rejects tokens (fail closed)
