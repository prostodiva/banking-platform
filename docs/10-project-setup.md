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

- using dependency Spring Web (manage inside pom.xml)
  (https://mvnrepository.com/artifact/org.springframework.boot/spring-boot-starter-web)
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
