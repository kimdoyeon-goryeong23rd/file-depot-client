package com.saltlux.filedepot.client;

import java.util.List;

import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.MediaType;
import org.springframework.web.reactive.function.client.WebClient;

import me.hanju.filedepot.api.dto.BatchDownloadRequest;
import me.hanju.filedepot.api.dto.ChunkDto;
import me.hanju.filedepot.api.dto.CommonResponseDto;
import me.hanju.filedepot.api.dto.ConfirmUploadRequest;
import me.hanju.filedepot.api.dto.DownloadUrlResponse;
import me.hanju.filedepot.api.dto.StorageItemDto;
import me.hanju.filedepot.api.dto.UploadUrlResponse;

/**
 * HTTP 기반 File Depot 클라이언트 구현.
 */
public class HttpFileDepotClient implements FileDepotClient {

  private final WebClient webClient;

  public HttpFileDepotClient(final WebClient.Builder webClientBuilder, final String baseUrl) {
    requireNonBlank(baseUrl, "baseUrl");
    this.webClient = webClientBuilder.baseUrl(baseUrl).build();
  }

  public HttpFileDepotClient(final String baseUrl) {
    requireNonBlank(baseUrl, "baseUrl");
    this.webClient = WebClient.builder().baseUrl(baseUrl).build();
  }

  @Override
  public UploadUrlResponse prepareUpload() {
    final CommonResponseDto<UploadUrlResponse> response = doPost(
        "/api/files/prepare-upload",
        null,
        new ParameterizedTypeReference<>() {
        });
    return unwrap(response);
  }

  @Override
  public StorageItemDto confirmUpload(final String id, final String fileName) {
    requireNonBlank(id, "id");
    requireMaxLength(fileName, 255, "fileName");
    final ConfirmUploadRequest request = new ConfirmUploadRequest(id, fileName);
    final CommonResponseDto<StorageItemDto> response = doPost(
        "/api/files/confirm-upload",
        request,
        new ParameterizedTypeReference<>() {
        });
    return unwrap(response);
  }

  @Override
  public StorageItemDto getFileMetadata(final String id, final boolean withContent) {
    requireNonBlank(id, "id");
    final String uri = withContent
        ? "/api/files/{id}?withContent=true"
        : "/api/files/{id}";
    final CommonResponseDto<StorageItemDto> response = doGet(
        uri,
        new ParameterizedTypeReference<>() {
        },
        id);
    return unwrap(response);
  }

  @Override
  public DownloadUrlResponse getDownloadUrl(final String id) {
    requireNonBlank(id, "id");
    final CommonResponseDto<DownloadUrlResponse> response = doGet(
        "/api/files/{id}/download-url",
        new ParameterizedTypeReference<>() {
        },
        id);
    return unwrap(response);
  }

  @Override
  public void deleteFiles(final List<String> ids) {
    requireNonEmptyIds(ids, "ids");
    final CommonResponseDto<Void> response = doPost(
        "/api/files/delete",
        ids,
        new ParameterizedTypeReference<>() {
        });
    unwrap(response);
  }

  @Override
  public byte[] downloadBatch(final List<String> ids) {
    requireNonEmptyIds(ids, "ids");
    final BatchDownloadRequest request = new BatchDownloadRequest(ids);
    return doPostForBytes("/api/files/download/batch", request);
  }

  @Override
  public List<ChunkDto> getChunks(final String id, final boolean withEmbedding) {
    requireNonBlank(id, "id");
    final String uri = withEmbedding
        ? "/api/files/{id}/chunks?withEmbedding=true"
        : "/api/files/{id}/chunks";
    final CommonResponseDto<List<ChunkDto>> response = doGet(
        uri,
        new ParameterizedTypeReference<>() {
        },
        id);
    return unwrap(response);
  }

  // ========== HTTP 요청 메서드 ==========

  private <T> CommonResponseDto<T> doGet(
      final String uri,
      final ParameterizedTypeReference<CommonResponseDto<T>> typeRef,
      final Object... uriVariables) {
    try {
      return webClient.get()
          .uri(uri, uriVariables)
          .retrieve()
          .bodyToMono(typeRef)
          .block();
    } catch (final FileDepotException e) {
      throw e;
    } catch (final Exception e) {
      throw new FileDepotClientException("unexpected: " + e.getMessage(), e);
    }
  }

  private <T> CommonResponseDto<T> doPost(
      final String uri,
      final Object body,
      final ParameterizedTypeReference<CommonResponseDto<T>> typeRef) {
    try {
      final WebClient.RequestBodySpec spec = webClient.post()
          .uri(uri)
          .contentType(MediaType.APPLICATION_JSON);

      if (body != null) {
        return spec.bodyValue(body)
            .retrieve()
            .bodyToMono(typeRef)
            .block();
      }
      return spec.retrieve()
          .bodyToMono(typeRef)
          .block();
    } catch (final FileDepotException e) {
      throw e;
    } catch (final Exception e) {
      throw new FileDepotClientException("unexpected: " + e.getMessage(), e);
    }
  }

  private byte[] doPostForBytes(final String uri, final Object body) {
    try {
      return webClient.post()
          .uri(uri)
          .contentType(MediaType.APPLICATION_JSON)
          .bodyValue(body)
          .retrieve()
          .bodyToMono(byte[].class)
          .block();
    } catch (final FileDepotException e) {
      throw e;
    } catch (final Exception e) {
      throw new FileDepotClientException("unexpected: " + e.getMessage(), e);
    }
  }

  // ========== 응답 처리 ==========

  private <T> T unwrap(final CommonResponseDto<T> response) {
    if (response == null) {
      throw new FileDepotException("No response from server");
    } else if (!response.success()) {
      throw new FileDepotException(response.message());
    } else {
      return response.data();
    }
  }

  // ========== 파라미터 검증 ==========

  private static void requireNonBlank(final String value, final String paramName) {
    if (value == null || value.isBlank()) {
      throw new IllegalArgumentException(paramName + " must not be null or blank");
    }
  }

  private static void requireMaxLength(final String value, final int maxLength, final String paramName) {
    if (value != null && value.length() > maxLength) {
      throw new IllegalArgumentException(paramName + " must not exceed " + maxLength + " characters");
    }
  }

  private static void requireNonEmptyIds(final List<String> ids, final String paramName) {
    if (ids == null || ids.isEmpty()) {
      throw new IllegalArgumentException(paramName + " must not be null or empty");
    }
    for (int i = 0; i < ids.size(); i++) {
      final String id = ids.get(i);
      if (id == null || id.isBlank()) {
        throw new IllegalArgumentException(paramName + "[" + i + "] must not be null or blank");
      }
    }
  }
}
