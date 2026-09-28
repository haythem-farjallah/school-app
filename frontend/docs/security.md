# Browser security

## Token storage (accepted risk)

`src/lib/token.ts` keeps the session in browser `localStorage`:

| Key            | Content                  | Lifetime                                                      |
| -------------- | ------------------------ | ------------------------------------------------------------- |
| `accessToken`  | JWT bearer token         | 15 minutes (`jwt.expiration.ms`)                              |
| `refreshToken` | JWT refresh token        | 24 hours, absolute: refresh issues a new access token only    |
| `userData`     | id, email, name, role    | until the session ends                                        |

**A successful same-origin XSS vulnerability could read and exfiltrate both bearer tokens.**
An attacker holding the refresh token can mint access tokens until it expires or the
account's sessions are revoked. The CSP below reduces the chance of XSS; it does not make
`localStorage` a safe place for credentials.

### Current mitigations

- Production CSP with `script-src 'self'` (no inline or eval scripts), served by the frontend nginx.
- No `dangerouslySetInnerHTML`, `eval` or `new Function` in application code.
- Short access token lifetime (15 minutes) and a 24-hour absolute refresh lifetime.
- Server-side `tokenVersion`: a successful password reset (e-mailed code) or authenticated
  password change (`/api/auth/change-password`) increments it, which invalidates every access
  and refresh token issued before it. Failed attempts leave it unchanged.
- Every request, refresh and WebSocket CONNECT reloads the account: tokens of suspended or
  deleted accounts, or issued before a password reset or change, are rejected.
- After an authenticated password change, on first login or from the profile, the frontend
  terminates the current local session and requires a fresh login with the new password.
- WebSocket sessions authenticate on CONNECT; after a suspension, deletion or `tokenVersion`
  change, their later frames and message deliveries are refused.

Limits of these mitigations:

- Logout clears browser storage only; it does not revoke the tokens server-side.
- A stolen refresh token stays usable for up to 24 hours unless `tokenVersion` changes.

### Future direction

Before the authentication design is finalised for public production, evaluate:

- access token held in memory only;
- refresh token in an `HttpOnly`, `Secure`, `SameSite` cookie.

That redesign must cover CSRF protection, login and page-load bootstrap, refresh,
logout (including server-side revocation), multi-tab behaviour and WebSocket authentication.

## Production headers

`nginx/security-headers.conf` is included by every nginx location that serves frontend files
(`/` and `/assets/`). `/api` and `/ws` responses carry the backend's own headers.

| Header                   | Value                                          |
| ------------------------ | ---------------------------------------------- |
| `Content-Security-Policy`| see below                                      |
| `X-Content-Type-Options` | `nosniff`                                      |
| `Referrer-Policy`        | `strict-origin-when-cross-origin`              |
| `X-Frame-Options`        | `DENY`                                         |
| `Permissions-Policy`     | `camera=(), microphone=(), geolocation=()`     |

Content Security Policy:

```text
default-src 'self';
base-uri 'self';
object-src 'none';
frame-ancestors 'none';
form-action 'self';
script-src 'self';
style-src 'self' 'unsafe-inline';
img-src 'self' blob:;
font-src 'self';
connect-src 'self'
```

Exceptions:

- `style-src 'unsafe-inline'`: components set inline `style` attributes. Removing it requires
  moving those styles into classes.
- `img-src blob:`: local previews of image files selected for upload.
- `connect-src 'self'` covers same-origin REST and the SockJS/WebSocket endpoint `/ws`,
  because the browser only talks to the nginx origin.

External images, media, frames and fonts are blocked. A learning resource or thumbnail whose
URL points to another site does not render inline; opening it in a new tab still works.

`Strict-Transport-Security` is not sent: the container serves plain HTTP and the TLS/ingress
topology is not decided yet. HSTS belongs to that configuration.
