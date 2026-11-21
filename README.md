# Online Document Storer

A Java 17 / Spring Boot 3 document management application with JWT authentication, role-based access, MySQL persistence and a browser-based frontend. Users can upload, organize, search, share and version documents.

## Features

- User registration and login with BCrypt password hashes and access/refresh JWTs
- User-owned documents, folders, metadata, tags and version history
- Private document downloads and token-based public share links
- File extension allow-list, upload size limit, generated storage filenames and upload-root path checks
- Optional AES-GCM encryption for selected uploads
- User activity records and admin endpoints protected by the ADMIN role
- Swagger / OpenAPI documentation

## Requirements

- JDK 17+
- Maven 3.8+
- MySQL 8.x
- Local storage for uploaded documents

## Local setup

### 1. Create the database

From the repository root:

```bash
mysql -u root -p < sql/schema.sql
```

The application uses JPA schema updates for development. Back up your database before applying application upgrades.

### 2. Configure environment variables

Copy `.env.example` as a local reference, then export the variables in the process environment that starts Spring Boot. Spring Boot does not automatically load dotenv files.

| Variable | Purpose |
|---|---|
| `DB_URL` | MySQL JDBC URL (defaults to local `document_storer_db`) |
| `DB_USERNAME` | MySQL account (defaults to `root`) |
| `DB_PASSWORD` | MySQL password |
| `JWT_SECRET` | Base64-encoded random signing key; must decode to at least 32 bytes |
| `ADMIN_EMAIL` | Optional initial administrator email |
| `ADMIN_PASSWORD` | Optional initial administrator password (12–72 UTF-8 bytes) |
| `ADMIN_NAME` | Optional administrator display name |
| `APP_CORS_ALLOWED_ORIGINS` | Exact comma-separated allowed browser origins |
| `APP_FILE_UPLOAD_DIR` | Upload directory (defaults to `./uploads`) |
| `APP_ENCRYPTION_ENABLED` | Set `true` to permit encrypted uploads |
| `ENCRYPTION_KEY` | Separate encryption secret (at least 32 characters) |

Generate a JWT key:

```bash
openssl rand -base64 32
```

Generate a separate encryption key only if encrypted uploads are needed:

```bash
openssl rand -hex 32
```

Keep both secrets separate, private and outside source control.

The first administrator is created only when both `ADMIN_EMAIL` and `ADMIN_PASSWORD` are set before the first application start. There is no built-in default admin password.

### 3. Start the app

```bash
mvn clean package
mvn spring-boot:run
```

Open [http://localhost:8080](http://localhost:8080).

Important pages:
- Landing: `/`
- Login: `/login.html`
- Register: `/register.html`
- Dashboard: `/dashboard.html`
- Swagger UI: `/swagger-ui.html`
- OpenAPI JSON: `/api-docs`

## Build and test

```bash
mvn --batch-mode clean verify
```

## API groups

- `/api/auth`: register, login, refresh and logout
- `/api/documents`: upload, list, download, delete, search, sharing, versions and metadata
- `/api/folders`: create, list, update and delete folders
- `/api/users/me`: profile, password, avatar, dashboard stats and activities
- `/api/admin`: user, document and system administration (ADMIN role)

Protected requests use:

```
Authorization: Bearer <access-token>
```

## File storage and encryption

Uploads are saved under `APP_FILE_UPLOAD_DIR`. Files receive generated server-side names, allowed extensions are checked, and normalized paths are constrained to the configured upload directory. The default upload limit is 50 MB.

Encrypted uploads are opt-in. Set both `APP_ENCRYPTION_ENABLED=true` and a unique `ENCRYPTION_KEY` (at least 32 characters). New encrypted files use AES-GCM with a random nonce and authentication tag. Files produced by the previous AES/ECB implementation can still be read by the compatibility path; re-upload or re-encrypt them to move to authenticated encryption.

## Security notes and known limitations

- CORS uses exact configured origins. Do not use wildcard origins with credentials.
- JWT signing configuration is required; startup validation rejects missing, invalid or short keys.
- No default admin password is created. Configure both administrator variables before first startup.
- Logout currently returns success to the client, but issued JWT access tokens remain valid until expiry. Add server-side token revocation if immediate invalidation is required.
- This is an educational application. Before production use, add HTTPS, rate limiting, malware scanning/content inspection for uploads, key rotation, database backups, retention/deletion policies, security monitoring and an independent security review.
