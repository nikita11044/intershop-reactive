## About The Project

A simple Spring Boot-based internet shop using reactive technologies and cloud-native components.

### Built With

- Spring WebFlux
- Thymeleaf
- PostgreSQL
- R2DBC
- Minio

### Prerequisites

- Java 21
- Gradle
- Docker

---

## Installation

1. **Clone the repository**

   ```bash
   git clone https://github.com/nikita11044/intershop-reactive.git
   cd intershop
   ```

2. **Start Docker containers**

   ```bash
   docker-compose up --build
   ```

   > 💡 You can customize the database and Minio credentials in `application.properties` and `docker-compose.yml`.

3. **Build the application**

   ```bash
   ./gradlew build
   ```

---

## Running the Application

You can run the app in **Docker** (default) or **locally**.

### Option 1: Run in Docker

Docker is pre-configured via `docker-compose.yml`.

```bash
docker-compose up --build
```

- PostgreSQL and Minio are started automatically.
- App will be available at [http://localhost:8080](http://localhost:8080).

If you change credentials in `application.properties`, make sure to reflect the same changes in `docker-compose.yml`.

---

### Option 2: Run Locally

1. **Update `application.properties`** to use your local services:

   ```properties
   spring.r2dbc.url=r2dbc:postgresql://localhost:5432/intershop_db
   spring.r2dbc.username=your_local_user
   spring.r2dbc.password=your_local_password

   intershop.file-storage.endpoint=http://localhost:9000
   ```

2. **Run the app**

   ```bash
   ./gradlew bootRun
   ```

- The application will be available at [http://localhost:8080](http://localhost:8080).

---

## Building the Application

To create an executable JAR:

```bash
./gradlew build
```

Run it with:

```bash
java -jar build/libs/intershop-reactive-0.0.1-SNAPSHOT.jar
```

---

## Testing the Application

To run unit and integration tests:

```bash
./gradlew test
```

Test reports are available at:

```
build/reports/tests/test/index.html
```

---

## Keycloak

The application uses Keycloak as OAuth provider

> ⚠️ **Note:**  
> You must manually create client in keycloak in advance for the application to function correctly.
> You can use example data from application.properties
