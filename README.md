## About The Project

A simple Spring Boot-based internet shop.

### Built With

* Spring
* Thymeleaf
* PostgreSQL
* Hibernate
* Minio

### Prerequisites

* Java 21
* Gradle
* Docker

### Installation

1. Clone the repo

```bash
git clone https://github.com/nikita11044/intershop.git
cd intershop
```

2. Start the Docker containers

```bash
docker-compose up --build
```

**Note:** You may configure database and minio credentials as you see fit. Just make sure they are the same as the ones in `application.properties`

3. Install Application Dependencies

```bash
./gradlew build
```

### Running the Application

You can run this application either in a Docker container or locally on your machine. The application is pre-configured to run in a Docker environment, but with minimal adjustments, it can be run locally as well.

#### Running the Application in Docker

The application is fully configured to be launched in Docker using the provided `docker-compose.yml` file. To get the application running in Docker:

1. **Clone the repository**:
    ```bash
    git clone https://github.com/nikita11044/intershop.git
    cd intershop
    ```

2. **Start the Docker containers**:
    ```bash
    docker-compose up --build
    ```
   This command will build and start the Docker containers, including the PostgreSQL database, Minio file storage, and the app itself.

    - The **PostgreSQL database** is configured to run with the credentials and settings defined in the `application.properties` file.
    - The **Minio** service is set up for file storage with the necessary access credentials.

3. The application will be available at `http://localhost:8080` by default.

   **Note:** If you wish to customize the database or Minio credentials, you can update the `application.properties` file. Be sure to adjust them in the `docker-compose.yml` file as well to match the new configuration.

#### Running the Application Locally

To run the application locally (outside of Docker), follow these steps:

1. **Clone the repository**:
    ```bash
    git clone https://github.com/nikita11044/intershop.git
    cd intershop
    ```

2. **Configure `application.properties`**:
   By default, the application is set to connect to Docker services (PostgreSQL and Minio). To run the app locally, you’ll need to update the `application.properties` file to match your local setup. Here’s a sample of what you might need to change:

    - Update the database connection settings to point to your local PostgreSQL instance.
    - Adjust the file storage endpoint to point to a local Minio instance (if using Minio locally) or another storage solution.

   For example:
    ```properties
    spring.datasource.url=jdbc:postgresql://localhost:5432/intershop_db
    spring.datasource.username=your_local_user
    spring.datasource.password=your_local_password

    intershop.file-storage.endpoint=http://localhost:9000
    ```

3. **Build the application**:
    ```bash
    ./gradlew build
    ```

4. **Run the application locally**:
   To run the app on your local machine, use the following Gradle command:
    ```bash
    ./gradlew bootRun
    ```
   The application will be available at `http://localhost:8080`.


### Building the Application 
To build the app into an executable JAR file, run the following command:
```bash
./gradlew build
```
The built JAR file will be located in the build/libs/ directory. You can run the JAR file with:
```bash
java -jar build/libs/intershop-0.0.1-SNAPSHOT.jar
```

### Testing the Application
In the root directory of the project, run the following command to execute all tests:
```bash
./gradlew test
```
This will run all unit and integration tests and provide a summary of the results in the terminal.

After running the tests, you can check the detailed test reports, which are available in:

```bash
build/reports/tests/test/index.html
```
