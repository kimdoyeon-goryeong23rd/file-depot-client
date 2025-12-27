package com.saltlux.filedepot.client;

import java.util.List;

import me.hanju.filedepot.api.dto.DownloadUrlResponse;
import me.hanju.filedepot.api.dto.StorageItemDto;
import me.hanju.filedepot.api.dto.UploadUrlResponse;

/**
 * File Depot 서비스 클라이언트 인터페이스.
 *
 * <p>
 * 모든 메서드는 다음 예외를 던질 수 있습니다:
 * <ul>
 * <li>{@link IllegalArgumentException} - 잘못된 파라미터 (null, blank, empty 등)</li>
 * <li>{@link FileDepotException} - 서버에서 반환한 에러 (success=false)</li>
 * <li>{@link FileDepotClientException} - 클라이언트 측 에러 (네트워크, 타임아웃 등)</li>
 * </ul>
 */
public interface FileDepotClient {

  /**
   * 파일 업로드를 위한 presigned URL과 UUID를 발급받습니다.
   *
   * @return 업로드 URL 정보 (id, uploadUrl, expirySeconds)
   * @throws FileDepotException       서버 에러
   * @throws FileDepotClientException 클라이언트 에러
   */
  UploadUrlResponse prepareUpload();

  /**
   * 파일 업로드 완료를 확인합니다.
   *
   * @param id 파일 UUID (not null, not blank)
   * @return 저장된 파일 메타데이터
   * @throws IllegalArgumentException id가 null이거나 blank인 경우
   * @throws FileDepotException       서버 에러
   * @throws FileDepotClientException 클라이언트 에러
   */
  StorageItemDto confirmUpload(String id);

  /**
   * 파일 메타데이터를 조회합니다.
   *
   * @param id 파일 UUID (not null, not blank)
   * @return 파일 메타데이터
   * @throws IllegalArgumentException id가 null이거나 blank인 경우
   * @throws FileDepotException       서버 에러 (파일 없음 등)
   * @throws FileDepotClientException 클라이언트 에러
   */
  StorageItemDto getFileMetadata(String id);

  /**
   * 파일 다운로드를 위한 presigned URL을 발급받습니다.
   *
   * @param id 파일 UUID (not null, not blank)
   * @return 다운로드 URL 정보 (downloadUrl, expirySeconds)
   * @throws IllegalArgumentException id가 null이거나 blank인 경우
   * @throws FileDepotException       서버 에러 (파일 없음 등)
   * @throws FileDepotClientException 클라이언트 에러
   */
  DownloadUrlResponse getDownloadUrl(String id);

  /**
   * 파일들을 삭제합니다 (soft delete).
   *
   * @param ids 삭제할 파일 UUID 목록 (not null, not empty, 각 요소 not blank)
   * @throws IllegalArgumentException ids가 null, empty이거나 blank 요소를 포함하는 경우
   * @throws FileDepotException       서버 에러
   * @throws FileDepotClientException 클라이언트 에러
   */
  void deleteFiles(List<String> ids);

  /**
   * 여러 파일을 ZIP으로 일괄 다운로드합니다.
   *
   * @param ids 다운로드할 파일 UUID 목록 (not null, not empty, 각 요소 not blank)
   * @return ZIP 파일 바이트 배열
   * @throws IllegalArgumentException ids가 null, empty이거나 blank 요소를 포함하는 경우
   * @throws FileDepotException       서버 에러
   * @throws FileDepotClientException 클라이언트 에러
   */
  byte[] downloadBatch(List<String> ids);

}
