# 📁 Online Document Storer

> Industry-level, full-stack Java web application for secure cloud document management.

---

## 🏗️ Project Structure

```
online-document-storer/
├── pom.xml                                         # Maven build config
├── sql/
│   └── schema.sql                                  # MySQL schema (optional manual setup)
├── uploads/                                        # Created at runtime
│   ├── documents/
│   ├── avatars/
│   ├── versions/
│   └── temp/
└── src/
    └── main/
        ├── java/com/docstorer/
        │   ├── OnlineDocumentStorerApplication.java
        │   ├── config/
        │   │   ├── AppConfig.java                  # ModelMapper, Swagger, Async
        │   │   ├── DataInitializer.java             # Creates admin + upload dirs
        │   │   └── SecurityConfig.java             # Spring Security + JWT + CORS
        │   ├── controller/
        │   │   ├── AuthController.java             # POST /api/auth/**
        │   │   ├── DocumentController.java         # /api/documents/**
        │   │   ├── FolderController.java           # /api/folders/**
        │   │   ├── UserController.java             # /api/users/me/**
        │   │   └── AdminController.java            # /api/admin/** (ADMIN role only)
        │   ├── dto/
        │   │   ├── request/                        # LoginRequest, RegisterRequest, etc.
        │   │   └── response/                       # ApiResponse, UserResponse, etc.
        │   ├── entity/
        │   │   ├── User.java
        │   │   ├── Document.java
        │   │   ├── DocumentMetadata.java
        │   │   ├── DocumentVersion.java
        │   │   ├── DocumentTag.java
        │   │   ├── DocumentShare.java
        │   │   ├── Folder.java
        │   │   └── ActivityLog.java
        │   ├── exception/
        │   │   ├── GlobalExceptionHandler.java
        │   │   ├── ResourceNotFoundException.java
        │   │   ├── UnauthorizedException.java
        │   │   └── FileStorageException.java
        │   ├── repository/                         # Spring Data JPA interfaces
        │   ├── security/
        │   │   ├── JwtTokenProvider.java
        │   │   ├── JwtAuthenticationFilter.java
        │   │   └── UserDetailsServiceImpl.java
        │   ├── service/
        │   │   ├── AuthService.java
        │   │   ├── DocumentService.java
        │   │   ├── FolderService.java
        │   │   ├── UserService.java
        │   │   └── impl/
        │   │       ├── AuthServiceImpl.java
        │   │       ├── DocumentServiceImpl.java
        │   │       ├── FolderServiceImpl.java
        │   │       └── UserServiceImpl.java
        │   └── util/
        │       └── FileStorageUtil.java
        └── resources/
            ├── application.properties
            └── static/                             # Frontend
                ├── index.html                      # Landing page
                ├── login.html
                ├── register.html
                ├── dashboard.html
                ├── documents.html
                ├── upload.html
                ├── search.html
                ├── folders.html
                ├── shared.html
                ├── activity.html
                ├── profile.html
                ├── css/
                │   └── styles.css
                └── js/
                    ├── app.js                      # Core utilities
                    ├── auth.js
                    ├── dashboard.js
                    ├── documents.js
                    ├── upload.js
                    ├── search.js
                    └── profile.js
```

---

## ⚙️ Tech Stack

| Layer        | Technology                     |
|--------------|-------------------------------|
| Backend      | Java 17 + Spring Boot 3.2     |
| Security     | Spring Security + JWT (JJWT)  |
| Database     | MySQL 8.x + Spring Data JPA   |
| Build Tool   | Maven                         |
| Frontend     | HTML5, CSS3, Vanilla JS       |
| API Docs     | Springdoc OpenAPI (Swagger)   |
| File Storage | Local filesystem (AES-256)    |

---

## 🗄️ Database Tables

| Table               | Description                            |
|---------------------|----------------------------------------|
| `users`             | Registered users with roles            |
| `documents`         | Uploaded file records and metadata     |
| `document_metadata` | Extended document properties           |
| `document_versions` | Version history per document           |
| `document_tags`     | Tag labels for documents               |
| `document_shares`   | Sharing records with permissions       |
| `folders`           | Nested folder structure                |
| `activity_logs`     | Full audit trail of all user actions   |

---

## 🔌 API Endpoints

### Auth  `/api/auth`
| Method | Path             | Description          |
|--------|------------------|----------------------|
| POST   | /register        | Register new user    |
| POST   | /login           | Login, get JWT       |
| POST   | /refresh         | Refresh access token |
| POST   | /logout          | Logout               |

### Documents  `/api/documents`
| Method | Path                              | Description              |
|--------|-----------------------------------|--------------------------|
| POST   | /upload                           | Upload document          |
| GET    | /                                 | List user's documents    |
| GET    | /{id}                             | Get document by ID       |
| GET    | /{id}/download                    | Download document        |
| DELETE | /{id}                             | Delete document          |
| GET    | /search                           | Search documents         |
| POST   | /{id}/share                       | Share document           |
| GET    | /shared/{token}                   | Get shared doc (public)  |
| GET    | /shared/{token}/download          | Download shared doc      |
| GET    | /{id}/versions                    | Get version history      |
| POST   | /{id}/versions/{vid}/restore      | Restore a version        |
| PUT    | /{id}/metadata                    | Update metadata          |
| PUT    | /{id}/move                        | Move to folder           |
| GET    | /recent                           | Get recent documents     |

### Folders  `/api/folders`
| Method | Path   | Description       |
|--------|--------|-------------------|
| POST   | /      | Create folder     |
| GET    | /      | List root folders |
| GET    | /{id}  | Get folder by ID  |
| PUT    | /{id}  | Update folder     |
| DELETE | /{id}  | Delete folder     |

### Users  `/api/users`
| Method | Path              | Description            |
|--------|-------------------|------------------------|
| GET    | /me               | Get own profile        |
| PUT    | /me               | Update profile         |
| POST   | /me/avatar        | Upload avatar          |
| PUT    | /me/password      | Change password        |
| GET    | /me/dashboard     | Dashboard stats        |
| GET    | /me/activities    | Activity log           |

### Admin  `/api/admin`  *(ADMIN role required)*
| Method | Path                       | Description         |
|--------|----------------------------|---------------------|
| GET    | /users                     | List all users      |
| GET    | /users/{id}                | Get user by ID      |
| PUT    | /users/{id}/toggle-active  | Enable/disable user |
| PUT    | /users/{id}/role           | Change user role    |
| GET    | /documents                 | All system docs     |
| GET    | /activities                | All activity logs   |
| GET    | /stats                     | System-wide stats   |

---

## 🚀 How to Run

### Prerequisites
- Java 17+
- Maven 3.8+
- MySQL 8.x running locally
- Git

### Step 1 — Clone
```bash
git clone <repo-url>
cd online-document-storer
```

### Step 2 — Configure Database
Edit `src/main/resources/application.properties`:
```properties
spring.datasource.url=jdbc:mysql://localhost:3306/document_storer_db?useSSL=false&serverTimezone=UTC&createDatabaseIfNotExist=true
spring.datasource.username=root
spring.datasource.password=YOUR_MYSQL_PASSWORD
```

### Step 3 — Run the App
```bash
mvn spring-boot:run
```
Or build a JAR first:
```bash
mvn clean package -DskipTests
java -jar target/online-document-storer-1.0.0.jar
```

### Step 4 — Access the App
| URL                                      | Description           |
|------------------------------------------|-----------------------|
| http://localhost:8080                    | Landing Page          |
| http://localhost:8080/login.html         | Login                 |
| http://localhost:8080/register.html      | Register              |
| http://localhost:8080/dashboard.html     | Dashboard             |
| http://localhost:8080/swagger-ui.html    | API Documentation     |
| http://localhost:8080/api-docs           | OpenAPI JSON          |

### Default Admin Account
```
Email:    admin@docstorer.com
Password: Admin@123
```
> ⚠️ **Change this immediately** in `application.properties` before deploying.

---

## 🔑 JWT Authentication Flow

1. Client sends `POST /api/auth/login` with credentials
2. Server validates and returns `accessToken` (24h) + `refreshToken` (7d)
3. Client stores tokens in `localStorage`
4. Every protected request includes `Authorization: Bearer <accessToken>`
5. When expired, client calls `POST /api/auth/refresh` with `Refresh-Token` header

---

## 🔒 Security Features

- **BCrypt** password hashing (strength 12)
- **JWT** stateless authentication
- **Role-based access** (USER / ADMIN)
- **AES-256** optional file encryption
- **Path traversal** prevention in file uploads
- **File type validation** whitelist
- **50 MB** max file size limit
- **CORS** configured for allowed origins
- **Spring Method Security** with `@PreAuthorize`

---

## 🎨 Frontend Pages

| Page              | File               | Description                     |
|-------------------|--------------------|---------------------------------|
| Landing           | index.html         | Public marketing page           |
| Login             | login.html         | JWT login form                  |
| Register          | register.html      | Account creation                |
| Dashboard         | dashboard.html     | Stats, recent docs, activity    |
| My Documents      | documents.html     | Grid/list view, filter, sort    |
| Upload            | upload.html        | Drag-and-drop, progress bars    |
| Search            | search.html        | Full-text + advanced filters    |
| Folders           | folders.html       | Create/manage folder structure  |
| Shared            | shared.html        | View your shared documents      |
| Activity Log      | activity.html      | Complete audit trail            |
| Profile/Settings  | profile.html       | Edit info, password, dark mode  |

---

## 🌙 Features Summary

- ✅ JWT authentication with refresh tokens
- ✅ Role-based access control (USER / ADMIN)
- ✅ Secure file upload (whitelist, size limit, path traversal guard)
- ✅ AES-256 optional file encryption
- ✅ Document version history + restore
- ✅ Document sharing via secure token links
- ✅ Folder management (nested)
- ✅ Tag system for documents
- ✅ Full-text search with filters (type, date range)
- ✅ Drag-and-drop upload with progress bars
- ✅ Dashboard with storage usage charts
- ✅ Activity audit logs
- ✅ Dark / light mode toggle
- ✅ Responsive design (mobile-friendly)
- ✅ Swagger / OpenAPI documentation
- ✅ Global exception handling
- ✅ Pagination and sorting
- ✅ Admin dashboard (user management, system stats)

---

## 📦 Environment Variables (Optional)

You can override `application.properties` with environment variables:

```bash
export SPRING_DATASOURCE_PASSWORD=secret
export APP_JWT_SECRET=your-256-bit-secret
export APP_FILE_UPLOAD_DIR=/var/docstorer/uploads
```

---

## 🐳 Docker (Optional)

```dockerfile
FROM eclipse-temurin:17-jre
WORKDIR /app
COPY target/online-document-storer-1.0.0.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java","-jar","app.jar"]
```

```bash
docker build -t docstorer .
docker run -p 8080:8080 \
  -e SPRING_DATASOURCE_URL=jdbc:mysql://host.docker.internal:3306/document_storer_db \
  -e SPRING_DATASOURCE_PASSWORD=yourpassword \
  docstorer
```

---

## 🤝 Contributing

1. Fork the repository
2. Create a feature branch: `git checkout -b feature/my-feature`
3. Commit your changes: `git commit -m "Add my feature"`
4. Push: `git push origin feature/my-feature`
5. Open a Pull Request

---

*Built with ❤️ using Spring Boot 3, MySQL, and Vanilla JS*
