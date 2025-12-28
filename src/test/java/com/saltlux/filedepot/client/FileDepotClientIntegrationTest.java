package com.saltlux.filedepot.client;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.List;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import me.hanju.filedepot.api.dto.DownloadUrlResponse;
import me.hanju.filedepot.api.dto.StorageItemDto;
import me.hanju.filedepot.api.dto.UploadUrlResponse;

/**
 * Integration tests for FileDepotClient using Testcontainers.
 *
 * <p>
 * Note: Tests that require actual file upload via presigned URL are limited
 * because MinIO returns internal container hostname in presigned URLs.
 */
@DisplayName("FileDepotClient Integration Tests")
class FileDepotClientIntegrationTest {

  private static FileDepotClient client;

  @BeforeAll
  static void setUp() {
    TestContainersConfig.startContainers();
    final String baseUrl = TestContainersConfig.getFileDepotBaseUrl();
    client = new HttpFileDepotClient(baseUrl);
  }

  /**
   * presigned URL을 변환 없이 그대로 반환.
   * file-depot 서버가 host.docker.internal로 MinIO에 연결하므로
   * presigned URL도 host.docker.internal 호스트로 서명됨.
   * Windows/Mac에서는 host.docker.internal이 호스트 머신으로 resolve됨.
   */
  private static String fixMinioUrl(final String url) {
    // URL 변환 없이 그대로 사용 (서명된 호스트와 일치해야 함)
    return url;
  }

  @AfterAll
  static void tearDown() {
    TestContainersConfig.stopContainers();
  }

  @Nested
  @DisplayName("prepareUpload()")
  class PrepareUploadTests {

    @Test
    @DisplayName("should return upload URL and ID")
    void shouldReturnUploadUrlAndId() {
      final UploadUrlResponse response = client.prepareUpload();

      assertThat(response).isNotNull();
      assertThat(response.id()).isNotBlank();
      assertThat(response.uploadUrl()).isNotBlank();
      assertThat(response.uploadUrl()).contains("X-Amz-Algorithm");
    }
  }

  @Nested
  @DisplayName("Error Handling - Parameter Validation")
  class ParameterValidationTests {

    @Test
    @DisplayName("confirmUpload() with null ID should throw IllegalArgumentException")
    void confirmUploadWithNullIdShouldThrowException() {
      assertThatThrownBy(() -> client.confirmUpload(null, "test.txt"))
          .isInstanceOf(IllegalArgumentException.class)
          .hasMessageContaining("id");
    }

    @Test
    @DisplayName("confirmUpload() with blank ID should throw IllegalArgumentException")
    void confirmUploadWithBlankIdShouldThrowException() {
      assertThatThrownBy(() -> client.confirmUpload("  ", "test.txt"))
          .isInstanceOf(IllegalArgumentException.class)
          .hasMessageContaining("id");
    }

    @Test
    @DisplayName("confirmUpload() with fileName exceeding 255 chars should throw IllegalArgumentException")
    void confirmUploadWithTooLongFileNameShouldThrowException() {
      final String longFileName = "a".repeat(256) + ".txt";
      assertThatThrownBy(() -> client.confirmUpload("valid-id", longFileName))
          .isInstanceOf(IllegalArgumentException.class)
          .hasMessageContaining("fileName");
    }

    @Test
    @DisplayName("getFileMetadata() with null ID should throw IllegalArgumentException")
    void getFileMetadataWithNullIdShouldThrowException() {
      assertThatThrownBy(() -> client.getFileMetadata(null, false))
          .isInstanceOf(IllegalArgumentException.class)
          .hasMessageContaining("id");
    }

    @Test
    @DisplayName("getChunks() with null ID should throw IllegalArgumentException")
    void getChunksWithNullIdShouldThrowException() {
      assertThatThrownBy(() -> client.getChunks(null, false))
          .isInstanceOf(IllegalArgumentException.class)
          .hasMessageContaining("id");
    }

    @Test
    @DisplayName("getDownloadUrl() with null ID should throw IllegalArgumentException")
    void getDownloadUrlWithNullIdShouldThrowException() {
      assertThatThrownBy(() -> client.getDownloadUrl(null))
          .isInstanceOf(IllegalArgumentException.class)
          .hasMessageContaining("id");
    }

    @Test
    @DisplayName("deleteFiles() with null list should throw IllegalArgumentException")
    void deleteFilesWithNullListShouldThrowException() {
      assertThatThrownBy(() -> client.deleteFiles(null))
          .isInstanceOf(IllegalArgumentException.class)
          .hasMessageContaining("ids");
    }

    @Test
    @DisplayName("deleteFiles() with empty list should throw IllegalArgumentException")
    void deleteFilesWithEmptyListShouldThrowException() {
      assertThatThrownBy(() -> client.deleteFiles(List.of()))
          .isInstanceOf(IllegalArgumentException.class)
          .hasMessageContaining("ids");
    }

    @Test
    @DisplayName("deleteFiles() with blank ID should throw IllegalArgumentException")
    void deleteFilesWithBlankIdShouldThrowException() {
      assertThatThrownBy(() -> client.deleteFiles(List.of("valid-id", "  ")))
          .isInstanceOf(IllegalArgumentException.class)
          .hasMessageContaining("ids[1]");
    }

    @Test
    @DisplayName("downloadBatch() with null list should throw IllegalArgumentException")
    void downloadBatchWithNullListShouldThrowException() {
      assertThatThrownBy(() -> client.downloadBatch(null))
          .isInstanceOf(IllegalArgumentException.class)
          .hasMessageContaining("ids");
    }

    @Test
    @DisplayName("downloadBatch() with empty list should throw IllegalArgumentException")
    void downloadBatchWithEmptyListShouldThrowException() {
      assertThatThrownBy(() -> client.downloadBatch(List.of()))
          .isInstanceOf(IllegalArgumentException.class)
          .hasMessageContaining("ids");
    }
  }

  @Nested
  @DisplayName("Error Handling - Server Errors")
  class ServerErrorTests {

    @Test
    @DisplayName("getFileMetadata() with non-existent ID should throw exception")
    void getFileMetadataWithNonExistentIdShouldThrowException() {
      assertThatThrownBy(() -> client.getFileMetadata("non-existent-uuid", false))
          .isInstanceOfAny(FileDepotException.class, FileDepotClientException.class);
    }

    @Test
    @DisplayName("getDownloadUrl() with non-existent ID should throw exception")
    void getDownloadUrlWithNonExistentIdShouldThrowException() {
      assertThatThrownBy(() -> client.getDownloadUrl("non-existent-uuid"))
          .isInstanceOfAny(FileDepotException.class, FileDepotClientException.class);
    }

    @Test
    @DisplayName("confirmUpload() with non-existent ID should throw exception")
    void confirmUploadWithNonExistentIdShouldThrowException() {
      assertThatThrownBy(() -> client.confirmUpload("non-existent-uuid", "test.txt"))
          .isInstanceOfAny(FileDepotException.class, FileDepotClientException.class);
    }

    @Test
    @DisplayName("getChunks() with non-existent ID should throw exception")
    void getChunksWithNonExistentIdShouldThrowException() {
      assertThatThrownBy(() -> client.getChunks("non-existent-uuid", false))
          .isInstanceOfAny(FileDepotException.class, FileDepotClientException.class);
    }
  }

  @Nested
  @DisplayName("Full Upload Flow Tests")
  class FullUploadFlowTests {

    private static final HttpClient httpClient = HttpClient.newHttpClient();

    @Test
    @DisplayName("Full flow: prepareUpload -> upload file -> confirmUpload -> getFileMetadata")
    void fullUploadFlowShouldWork() throws Exception {
      // 1. Prepare upload
      final UploadUrlResponse prepareResponse = client.prepareUpload();
      assertThat(prepareResponse).isNotNull();
      assertThat(prepareResponse.id()).isNotBlank();
      assertThat(prepareResponse.uploadUrl()).isNotBlank();

      final String fileId = prepareResponse.id();
      final String originalUrl = prepareResponse.uploadUrl();
      System.out.println("Original Upload URL: " + originalUrl);
      final String uploadUrl = fixMinioUrl(originalUrl);
      System.out.println("Fixed Upload URL: " + uploadUrl);

      // 2. Upload file to presigned URL
      final byte[] fileContent = "Hello, File Depot!".getBytes(StandardCharsets.UTF_8);
      final HttpRequest uploadRequest = HttpRequest.newBuilder()
          .uri(URI.create(uploadUrl))
          .PUT(HttpRequest.BodyPublishers.ofByteArray(fileContent))
          .header("Content-Type", "text/plain")
          .build();

      final HttpResponse<String> uploadResponse = httpClient.send(uploadRequest,
          HttpResponse.BodyHandlers.ofString());
      System.out.println("Upload status: " + uploadResponse.statusCode());
      System.out.println("Upload body: " + uploadResponse.body());
      assertThat(uploadResponse.statusCode())
          .as("Upload failed with body: " + uploadResponse.body())
          .isEqualTo(200);

      // 3. Confirm upload
      final StorageItemDto confirmResult = client.confirmUpload(fileId, "test-file.txt");
      assertThat(confirmResult).isNotNull();
      assertThat(confirmResult.id()).isEqualTo(fileId);
      assertThat(confirmResult.fileName()).isEqualTo("test-file.txt");
      assertThat(confirmResult.size()).isEqualTo(fileContent.length);

      // 4. Get file metadata
      final StorageItemDto metadata = client.getFileMetadata(fileId, false);
      assertThat(metadata).isNotNull();
      assertThat(metadata.id()).isEqualTo(fileId);
      assertThat(metadata.fileName()).isEqualTo("test-file.txt");
    }

    @Test
    @DisplayName("confirmUpload with null fileName should use UUID as fileName")
    void confirmUploadWithNullFileNameShouldUseUuid() throws Exception {
      // 1. Prepare upload
      final UploadUrlResponse prepareResponse = client.prepareUpload();
      final String fileId = prepareResponse.id();

      // 2. Upload file
      final byte[] fileContent = "Test content".getBytes(StandardCharsets.UTF_8);
      final HttpRequest uploadRequest = HttpRequest.newBuilder()
          .uri(URI.create(fixMinioUrl(prepareResponse.uploadUrl())))
          .PUT(HttpRequest.BodyPublishers.ofByteArray(fileContent))
          .header("Content-Type", "text/plain")
          .build();
      httpClient.send(uploadRequest, HttpResponse.BodyHandlers.discarding());

      // 3. Confirm upload with null fileName
      final StorageItemDto confirmResult = client.confirmUpload(fileId, null);
      assertThat(confirmResult).isNotNull();
      assertThat(confirmResult.id()).isEqualTo(fileId);
      // fileName should be UUID when null is passed
      assertThat(confirmResult.fileName()).isEqualTo(fileId);
    }

    @Test
    @DisplayName("getDownloadUrl should return valid presigned URL")
    void getDownloadUrlShouldReturnValidUrl() throws Exception {
      // 1. Upload a file first
      final UploadUrlResponse prepareResponse = client.prepareUpload();
      final String fileId = prepareResponse.id();

      final byte[] fileContent = "Download test content".getBytes(StandardCharsets.UTF_8);
      final HttpRequest uploadRequest = HttpRequest.newBuilder()
          .uri(URI.create(fixMinioUrl(prepareResponse.uploadUrl())))
          .PUT(HttpRequest.BodyPublishers.ofByteArray(fileContent))
          .header("Content-Type", "text/plain")
          .build();
      httpClient.send(uploadRequest, HttpResponse.BodyHandlers.discarding());
      client.confirmUpload(fileId, "download-test.txt");

      // 2. Get download URL
      final DownloadUrlResponse downloadResponse = client.getDownloadUrl(fileId);
      assertThat(downloadResponse).isNotNull();
      assertThat(downloadResponse.downloadUrl()).isNotBlank();
      assertThat(downloadResponse.downloadUrl()).contains("X-Amz-Algorithm");

      // 3. Download and verify content
      final HttpRequest downloadRequest = HttpRequest.newBuilder()
          .uri(URI.create(fixMinioUrl(downloadResponse.downloadUrl())))
          .GET()
          .build();
      final HttpResponse<byte[]> downloadResult = httpClient.send(downloadRequest,
          HttpResponse.BodyHandlers.ofByteArray());
      assertThat(downloadResult.statusCode()).isEqualTo(200);
      assertThat(downloadResult.body()).isEqualTo(fileContent);
    }

    @Test
    @DisplayName("deleteFiles should soft delete files")
    void deleteFilesShouldWork() throws Exception {
      // 1. Upload a file
      final UploadUrlResponse prepareResponse = client.prepareUpload();
      final String fileId = prepareResponse.id();

      final byte[] fileContent = "Delete test".getBytes(StandardCharsets.UTF_8);
      final HttpRequest uploadRequest = HttpRequest.newBuilder()
          .uri(URI.create(fixMinioUrl(prepareResponse.uploadUrl())))
          .PUT(HttpRequest.BodyPublishers.ofByteArray(fileContent))
          .header("Content-Type", "text/plain")
          .build();
      httpClient.send(uploadRequest, HttpResponse.BodyHandlers.discarding());
      client.confirmUpload(fileId, "delete-test.txt");

      // 2. Delete the file
      client.deleteFiles(List.of(fileId));

      // 3. Verify file is deleted (should throw exception)
      assertThatThrownBy(() -> client.getFileMetadata(fileId, false))
          .isInstanceOfAny(FileDepotException.class, FileDepotClientException.class);
    }
  }
}
