## Redis Cart Service

A demo Spring Boot application showcasing how to use Redis as the primary data store for a shopping cart service.
It includes session-based authentication, cart persistence with Redis hashes, RediSearch for querying, and cart expiration via hash-field TTL.

## Features

- **Session-based authentication** with Spring Security and Redis.
- **Products API** backed by Redis JSON and RediSearch.
- **Shopping cart API** using Redis hashes.
- **Session expiration** with Redis hash-field TTL (5 minutes idle).
- **Admin API** to query carts with more than 10 products.
- **Swagger/OpenAPI** integration for API exploration.
- **Vue.js frontend** for easy testing and demonstration.

---

## Prerequisites

- Java 21+
- Maven 3.9+
- Redis 8.0+ (with RediSearch module enabled)
- Node.js 18+ and npm 9+ (for the frontend)

---

## Running the Backend

1. Start a Redis 8+ instance with RediSearch enabled.  
   For example using Docker:

   ```bash
   docker run -it --rm -p 6379:6379 redis/redis-stack-server:latest
   ```

   This exposes Redis on port 6379 with RediSearch available.

2. Configure Redis in `application.yml` if needed (default is `localhost:6379`).

3. Build and run the Spring Boot app:

   ```bash
   ./mvnw clean package
   java -jar target/rediscartservice-0.0.1-SNAPSHOT.jar
   ```

4. Open Swagger UI at:  
   [http://localhost:8080/swagger-ui.html](http://localhost:8080/swagger-ui.html)

   Default test users are:

   - `user / user123` (ROLE_USER)
   - `admin / admin123` (ROLE_ADMIN)

---

## Running the Frontend

1. Go to the `frontend` folder:

   ```bash
   cd frontend
   ```

2. Install dependencies:

   ```bash
   npm install
   ```

3. Start the dev server:

   ```bash
   npm run dev
   ```

   By default, Vite serves the app at [http://localhost:5173](http://localhost:5173).

---

## Using the Application

- Open [http://localhost:5173](http://localhost:5173).
- Login as `user` or `admin`.
- As a **user**:
  - Browse products.
  - Add/remove items from your cart.
  - Restore carts from previous sessions.
- As an **admin**:
  - Manage products (create, update, delete).
  - View all sessions with more than 10 products.
  - Inspect the contents of any session’s cart.
