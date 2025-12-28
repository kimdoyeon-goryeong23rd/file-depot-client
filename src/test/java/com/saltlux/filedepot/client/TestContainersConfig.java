package com.saltlux.filedepot.client;

import java.time.Duration;

import org.testcontainers.containers.FixedHostPortGenericContainer;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.Network;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.utility.DockerImageName;

/**
 * Testcontainers configuration for file-depot-client integration tests.
 *
 * <p>
 * Sets up MariaDB, MinIO, and file-depot containers in a shared network.
 */
public final class TestContainersConfig {

  private static final String MARIADB_IMAGE = "mariadb:11.2";
  private static final String MINIO_IMAGE = "minio/minio:latest";
  private static final String FILE_DEPOT_IMAGE = "kimdoyeon/file-depot:0.2.0";

  private static final String MINIO_USER = "minioadmin";
  private static final String MINIO_PASSWORD = "minioadmin";
  private static final String MINIO_BUCKET = "file-depot";
  private static final int MINIO_FIXED_PORT = 19000;

  private static final Network NETWORK = Network.newNetwork();

  private static GenericContainer<?> mariaDbContainer;
  private static GenericContainer<?> minioContainer;
  private static GenericContainer<?> fileDepotContainer;

  private TestContainersConfig() {
  }

  public static synchronized void startContainers() {
    if (mariaDbContainer == null) {
      mariaDbContainer = createMariaDbContainer();
      mariaDbContainer.start();
    }

    if (minioContainer == null) {
      minioContainer = createMinioContainer();
      minioContainer.start();
    }

    if (fileDepotContainer == null) {
      fileDepotContainer = createFileDepotContainer();
      fileDepotContainer.start();
    }
  }

  public static String getFileDepotBaseUrl() {
    return "http://" + fileDepotContainer.getHost() + ":" + fileDepotContainer.getMappedPort(8080);
  }

  @SuppressWarnings("resource")
  private static GenericContainer<?> createMariaDbContainer() {
    return new GenericContainer<>(DockerImageName.parse(MARIADB_IMAGE))
        .withExposedPorts(3306)
        .withEnv("MYSQL_ROOT_PASSWORD", "root")
        .withEnv("MYSQL_DATABASE", "file_depot")
        .withNetwork(NETWORK)
        .withNetworkAliases("mariadb")
        .waitingFor(Wait.forLogMessage(".*ready for connections.*", 2)
            .withStartupTimeout(Duration.ofMinutes(2)));
  }

  @SuppressWarnings({ "resource", "deprecation" })
  private static GenericContainer<?> createMinioContainer() {
    return new FixedHostPortGenericContainer<>(MINIO_IMAGE)
        .withFixedExposedPort(MINIO_FIXED_PORT, 9000)
        .withExposedPorts(9000, 9001)
        .withEnv("MINIO_ROOT_USER", MINIO_USER)
        .withEnv("MINIO_ROOT_PASSWORD", MINIO_PASSWORD)
        .withEnv("MINIO_SERVER_URL", "http://localhost:" + MINIO_FIXED_PORT)
        .withCommand("server", "/data", "--console-address", ":9001")
        .withNetwork(NETWORK)
        .withNetworkAliases("minio")
        .waitingFor(Wait.forHttp("/minio/health/live").forPort(9000).forStatusCode(200));
  }

  @SuppressWarnings("resource")
  private static GenericContainer<?> createFileDepotContainer() {
    return new GenericContainer<>(DockerImageName.parse(FILE_DEPOT_IMAGE))
        .withNetwork(NETWORK)
        .withExposedPorts(8080, 8081)
        // Spring profile
        .withEnv("SPRING_PROFILES_ACTIVE", "prod")
        // Database (application-prod.yml)
        .withEnv("MARIADB_URL", "jdbc:mariadb://mariadb:3306/file_depot")
        .withEnv("MARIADB_USER", "root")
        .withEnv("MARIADB_PASSWORD", "root")
        .withEnv("DDL_AUTO", "update")
        .withEnv("DB_POOL_SIZE", "5")
        // MinIO (application-prod.yml)
        // Use host.docker.internal so presigned URLs work from host machine
        .withEnv("MINIO_URL", "http://host.docker.internal:" + MINIO_FIXED_PORT)
        .withEnv("MINIO_ACCESS_KEY", MINIO_USER)
        .withEnv("MINIO_SECRET_KEY", MINIO_PASSWORD)
        .withEnv("MINIO_BUCKET", MINIO_BUCKET)
        // Parsekit (application.yml)
        .withEnv("PARSEKIT_SCENARIO", "disabled")
        // EmbedKit (application.yml)
        .withEnv("EMBEDKIT_PROVIDER", "none")
        // Processing (application-prod.yml overrides to false)
        .withEnv("PROCESSING_BATCH_ENABLED", "false")
        // Consul disabled
        .withEnv("CONSUL_ENABLED", "false")
        .dependsOn(mariaDbContainer, minioContainer)
        .waitingFor(Wait.forHttp("/actuator/health").forPort(8081).forStatusCode(200)
            .withStartupTimeout(Duration.ofMinutes(3)));
  }

  public static synchronized void stopContainers() {
    if (fileDepotContainer != null) {
      fileDepotContainer.stop();
      fileDepotContainer = null;
    }
    if (minioContainer != null) {
      minioContainer.stop();
      minioContainer = null;
    }
    if (mariaDbContainer != null) {
      mariaDbContainer.stop();
      mariaDbContainer = null;
    }
  }
}
