package com.saltlux.filedepot.client;

/**
 * File Depot 클라이언트 측 에러.
 * 네트워크 오류, 타임아웃, 직렬화 실패 등 서버 응답 외의 문제.
 */
public class FileDepotClientException extends RuntimeException {

  public FileDepotClientException(final String message) {
    super(message);
  }

  public FileDepotClientException(final String message, final Throwable cause) {
    super(message, cause);
  }
}
