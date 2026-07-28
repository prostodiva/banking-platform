# 10 Project Setup

## Backend

- Java JDK - Amazon Corretto 21
- Maven (mvn) - java ecosystem; equivalent of tools like npm; helps with dependency management
- Java Extension Pack(VSC); in Zed (Java Language Server (jdtls))
- Spring Initializer (https://start.spring.io/)

to compile (into the target directory) and run the spring boot app:

```
mvn clean spring-boot:run
```

clean will remove the target directory before compiling

### launching HTTP server

- using dependency Spring WebMvc(manage inside pom.xml)
  (spring-boot-starter-webmvc)
  use the same version as spring boot starter parent

HTTP server - Tomcat is running on port 8080
Spring Boot - http://localhost:8080

## Frontend

```
npm create vite@latest . --template react
npm install
npm run dev
```

Vite - http://localhost:5173

## Make both servers communicate

using Vite proxy (http://localhost:5173/api/* will be proxied to http://localhost:8080/api/*)

- add proxy config to vite.config.js
  ```
  server: {
    proxy: {
      "/api": "http://localhost:8080",
    },
  },
  ```
- add a ping endpoint to the backend to test the connection
  ```
  @GetMapping("/api/ping")
  public String ping() {
    return "pong";
  }
  ```
- call the api from the frontend to test the connection
  ```
  const response = await fetch("/api/ping");
  ```

localhost:8080/api/ping or localhost:5173
recieve "pong" from backend server

## Spring-Boot DevTools

picks up changes without restarting
download dependency from Maven Central

## Docker (local infrastructure)

Apps run natively (DevTools / Vite hot reload); only infrastructure runs in Docker.

in the project root, create docker-compose.yml

```
cp .env.example .env          # first time only; .env is gitignored
docker compose up -d          # postgres
docker compose --profile cache up -d    # + redis (later)
docker compose --profile events up -d   # + kafka (later)
```

App Dockerfiles (multi-stage builds) and a full-stack compose file come later,
once there are real features to containerize.

## Frontend state management (planned)

Redux Toolkit + RTK Query, added when the first feature is built:

```
npm install @reduxjs/toolkit react-redux
```

- one `baseApi` (createApi + fetchBaseQuery("/api")) in `src/shared/api/`
- each feature injects its own endpoints via `baseApi.injectEndpoints` and keeps
  its `slice.ts` / `api.ts` / `types.ts` inside `src/features/<name>/`
- store wiring lives in `src/app/store.ts`

Structure details: docs/08-system-architecture.md

## Backend Dependencies

- spring-boot-starter-data-jpa (maps java entities to database tables)

> JPA (Jakarta Persistence API) is a specification: a set of Java interfaces and annotations (@Entity, @Id, @Column, @Table, @Version, EntityManager...) that define a standard way to say "this Java object corresponds to this database row." It answers one question: how do objects get in and out of tables?
> JPA itself does nothing — it's just interfaces. Hibernate is the implementation that actually does the work (generates SQL, tracks which fields changed, manages the connection). When Spring Boot autoconfigures JPA, it's really wiring up Hibernate underneath.

- spring-boot-starter-validation (Bean validation on DTOs)
- spring-boot-starter-flyway (database migrations)
- flyway-database-postgresql (PostgreSQL database driver for Flyway)
- JDBC Driver (PostgreSQL)
- spring-boot-testcontainers (Testcontainers for integration testing)
- testcontainers-postgresql (PostgreSQL Testcontainer)
- spring-boot-resttestclient
