package practicum.payment.util;

import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.utility.MountableFile;

public class PostgreSQLTestContainer extends PostgreSQLContainer<PostgreSQLTestContainer> {

    private static final String IMAGE_VERSION = "postgres:15";
    private static PostgreSQLTestContainer container;

    private PostgreSQLTestContainer() {
        super(IMAGE_VERSION);
        withCopyFileToContainer(
                MountableFile.forClasspathResource("init-db.sql"),
                "/docker-entrypoint-initdb.d/init-db.sql"
        );
    }

    public static PostgreSQLTestContainer getInstance() {
        if (container == null) {
            container = new PostgreSQLTestContainer();
            container.start();
        }
        return container;
    }
}

