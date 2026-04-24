# EmailAuto

Bulk email web application built with Spring Boot, PostgreSQL, plain HTML/CSS/JavaScript, Google OAuth 2.0, Apache POI, Cloudinary, and the Gmail API.

## Folder Structure

```text
.
|-- Dockerfile
|-- pom.xml
|-- README.md
`-- src
    `-- main
        |-- java/com/emailauto
        |   |-- config
        |   |-- domain
        |   |-- repository
        |   |-- service
        |   `-- web
        `-- resources
            |-- application.yml
            |-- application-postgres.properties
            |-- schema-postgres.sql
            `-- static
```

## Google OAuth 2.0 Setup

1. Open Google Cloud Console and create or select a project.
2. Go to APIs & Services > Library and enable Gmail API.
3. Go to APIs & Services > OAuth consent screen.
4. Choose External for development, add the app name, support email, and developer contact.
5. Add the Gmail send scope:

```text
https://www.googleapis.com/auth/gmail.send
```

6. Add test users while the consent screen is in testing mode.
7. Go to APIs & Services > Credentials > Create Credentials > OAuth client ID.
8. Choose Web application.
9. Add the redirect URI:

```text
http://localhost:8080/auth/google/callback
```

10. Copy the client ID and client secret into environment variables.

## OAuth Code Map

Dependency:

```xml
<dependency>
    <groupId>com.google.api-client</groupId>
    <artifactId>google-api-client</artifactId>
    <version>2.7.0</version>
</dependency>
<dependency>
    <groupId>com.google.oauth-client</groupId>
    <artifactId>google-oauth-client-jetty</artifactId>
    <version>1.36.0</version>
</dependency>
```

Configuration:

```yaml
app:
  oauth:
    google:
      client-id: ${GOOGLE_CLIENT_ID}
      client-secret: ${GOOGLE_CLIENT_SECRET}
      redirect-uri: ${GOOGLE_REDIRECT_URI:${app.base-url}/auth/google/callback}
```

Login redirect is handled by `AuthController`:

```java
@GetMapping("/auth/google")
public void login(HttpSession session, HttpServletResponse response) throws IOException {
    String state = newState();
    session.setAttribute("OAUTH_STATE", state);
    response.sendRedirect(googleOAuthService.buildAuthorizationUrl(state));
}
```

The authorization URL requests offline access so Google can return a refresh token:

```java
return new GoogleAuthorizationCodeRequestUrl(
        clientId,
        redirectUri,
        List.of(GmailScopes.GMAIL_SEND, "openid", "email", "profile"))
        .setAccessType("offline")
        .setApprovalPrompt("force")
        .setState(state)
        .build();
```

Callback handling:

```java
@GetMapping("/auth/google/callback")
public void callback(@RequestParam String code, @RequestParam String state, HttpSession session, HttpServletResponse response)
        throws IOException, GeneralSecurityException {
    String expectedState = (String) session.getAttribute("OAUTH_STATE");
    if (expectedState == null || !expectedState.equals(state)) {
        response.sendError(400, "Invalid OAuth state");
        return;
    }
    UserAccount user = googleOAuthService.exchangeCodeAndUpsertUser(code);
    session.setAttribute(GoogleOAuthService.SESSION_USER_ID, user.getId());
    response.sendRedirect("/");
}
```

Access token, refresh token, and email extraction happen in `GoogleOAuthService.exchangeCodeAndUpsertUser`:

```java
GoogleTokenResponse tokenResponse = new GoogleAuthorizationCodeTokenRequest(
        httpTransport, jsonFactory, clientId, clientSecret, code, redirectUri)
        .execute();

GoogleIdToken idToken = verifier.verify(tokenResponse.getIdToken());
String email = idToken.getPayload().getEmail();
String accessToken = tokenResponse.getAccessToken();
String refreshToken = tokenResponse.getRefreshToken();
```

The app encrypts both tokens with AES-GCM before saving them in PostgreSQL. Tokens are never sent to the browser.

## Cloudinary Integration

Dependency:

```xml
<dependency>
    <groupId>com.cloudinary</groupId>
    <artifactId>cloudinary-http45</artifactId>
    <version>1.39.0</version>
</dependency>
```

Config:

```yaml
app:
  cloudinary:
    cloud-name: ${CLOUDINARY_CLOUD_NAME}
    api-key: ${CLOUDINARY_API_KEY}
    api-secret: ${CLOUDINARY_API_SECRET}
    folder: ${CLOUDINARY_FOLDER:email-auto/excel}
```

Service class:

- `CloudinaryExcelService.uploadExcel(file)` uploads `.xlsx` as a Cloudinary raw asset and returns `secure_url`.
- `CloudinaryExcelService.downloadAndProcess(fileUrl)` downloads the Cloudinary URL and passes the stream to `ExcelContactParser`.

Controller endpoints:

```text
POST /api/files/excel
form-data: file=<contacts.xlsx>
returns: { "fileUrl": "https://res.cloudinary.com/..." }

POST /api/files/excel/process
form-data: fileUrl=https://res.cloudinary.com/...
returns: { "count": 2, "contacts": [...] }
```

## PostgreSQL Setup

Create the database:

```sql
create database email_auto;
```

Run the schema in `src/main/resources/schema-postgres.sql`. It creates:

```text
users
campaigns
emails
```

Spring Boot PostgreSQL config is in `src/main/resources/application-postgres.properties`:

```properties
spring.datasource.url=${DATABASE_URL:jdbc:postgresql://localhost:5432/email_auto}
spring.datasource.username=${DATABASE_USERNAME:postgres}
spring.datasource.password=${DATABASE_PASSWORD:postgres}
spring.datasource.driver-class-name=org.postgresql.Driver
spring.jpa.hibernate.ddl-auto=validate
spring.jpa.database-platform=org.hibernate.dialect.PostgreSQLDialect
spring.jpa.open-in-view=false
```

Run with the profile:

```powershell
$env:SPRING_PROFILES_ACTIVE="postgres"
$env:DATABASE_URL="jdbc:postgresql://localhost:5432/email_auto"
$env:DATABASE_USERNAME="postgres"
$env:DATABASE_PASSWORD="postgres"
mvn spring-boot:run
```

## Required Environment Variables

```powershell
$env:GOOGLE_CLIENT_ID="your-client-id"
$env:GOOGLE_CLIENT_SECRET="your-client-secret"
$env:GOOGLE_REDIRECT_URI="http://localhost:8080/auth/google/callback"
$env:APP_BASE_URL="http://localhost:8080"
$env:TOKEN_ENCRYPTION_KEY="replace-with-a-long-random-secret"
$env:DATABASE_URL="jdbc:postgresql://localhost:5432/email_auto"
$env:DATABASE_USERNAME="postgres"
$env:DATABASE_PASSWORD="postgres"
$env:CLOUDINARY_CLOUD_NAME="your-cloud-name"
$env:CLOUDINARY_API_KEY="your-api-key"
$env:CLOUDINARY_API_SECRET="your-api-secret"
```

## Docker

Build:

```powershell
docker build -t email-auto .
```

Run:

```powershell
docker run --rm -p 8080:8080 `
  -e PORT=8080 `
  -e SPRING_PROFILES_ACTIVE=postgres `
  -e DATABASE_URL="jdbc:postgresql://host.docker.internal:5432/email_auto" `
  -e DATABASE_USERNAME="postgres" `
  -e DATABASE_PASSWORD="postgres" `
  -e GOOGLE_CLIENT_ID="your-client-id" `
  -e GOOGLE_CLIENT_SECRET="your-client-secret" `
  -e GOOGLE_REDIRECT_URI="http://localhost:8080/auth/google/callback" `
  -e TOKEN_ENCRYPTION_KEY="replace-with-a-long-random-secret" `
  -e CLOUDINARY_CLOUD_NAME="your-cloud-name" `
  -e CLOUDINARY_API_KEY="your-api-key" `
  -e CLOUDINARY_API_SECRET="your-api-secret" `
  email-auto
```

## Excel Format

The first sheet must have a header row with these columns:

```text
name | email | company
```

The email template supports `{{name}}`, `{{email}}`, and `{{company}}`.
