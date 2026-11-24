# SocietyCrave

SocietyCrave is a homemade-food discovery and membership application for residents and home chefs.

## Run locally

The Angular application runs on `http://localhost:4200`:

```bash
cd frontend
npm start
```

The Spring Boot backend runs on `http://localhost:8080`:

```bash
cd backend
DB_URL=jdbc:postgresql://localhost:5432/oursociety \
DB_USERNAME=raghavagrawal DB_PASSWORD='your-password' \
GOOGLE_CLIENT_ID='your-client-id' GOOGLE_CLIENT_SECRET='your-client-secret' \
mvn spring-boot:run
```

Google OAuth must have `http://localhost:8080/login/oauth2/code/google` configured as an authorized redirect URI. The frontend uses an HttpOnly server session cookie; tokens are not stored in localStorage or URL parameters.

## Database safety and migrations

The backend uses `spring.jpa.hibernate.ddl-auto=validate` and does not create, migrate, drop, or truncate tables. It does not have Flyway or Liquibase configured, so SQL migration files are not executed automatically.

Before backend startup, review and manually apply `backend/src/main/resources/db/migration/V1__create_societycrave_auth_and_membership.sql` and `backend/src/main/resources/db/migration/V2__create_chef_listing_order.sql` to each approved PostgreSQL database in version order. Apply migrations using the approved database process only after reviewing the target environment.

## Current implementation

The Angular landing page, authentication entry points, callback shell, society search, and onboarding route are implemented. The backend includes OAuth2/OIDC integration, current-user lookup, society search/suggestions, and self-declared membership APIs with DTO validation and duplicate membership protection.

The app is structured for local development and requires real Google credentials for an end-to-end OAuth test. Cinematic MP4 and poster assets should be supplied under `frontend/public/assets/videos/`; the landing page currently uses a video-ready visual shell without pretending that promotional imagery represents registered chefs.
