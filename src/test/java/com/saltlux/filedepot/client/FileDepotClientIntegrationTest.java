package com.saltlux.filedepot.client;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

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
      assertThatThrownBy(() -> client.confirmUpload(null))
          .isInstanceOf(IllegalArgumentException.class)
          .hasMessageContaining("id");
    }

    @Test
    @DisplayName("confirmUpload() with blank ID should throw IllegalArgumentException")
    void confirmUploadWithBlankIdShouldThrowException() {
      assertThatThrownBy(() -> client.confirmUpload("  "))
          .isInstanceOf(IllegalArgumentException.class)
          .hasMessageContaining("id");
    }

    @Test
    @DisplayName("getFileMetadata() with null ID should throw IllegalArgumentException")
    void getFileMetadataWithNullIdShouldThrowException() {
      assertThatThrownBy(() -> client.getFileMetadata(null))
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
      assertThatThrownBy(() -> client.getFileMetadata("non-existent-uuid"))
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
      assertThatThrownBy(() -> client.confirmUpload("non-existent-uuid"))
          .isInstanceOfAny(FileDepotException.class, FileDepotClientException.class);
    }
  }
}
