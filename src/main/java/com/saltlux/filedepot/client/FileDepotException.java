package com.saltlux.filedepot.client;

/**
 * File Depot 서버에서 반환한 에러.
 * CommonResponseDto.success=false 인 경우 발생.
 */
public class FileDepotException extends RuntimeException {

  private final String code;
  private final String errorMessage;

  public FileDepotException(final String code, final String message) {
    super(buildMessage(code, message));
    this.code = code;
    this.errorMessage = message;
  }

  public FileDepotException(final String message) {
    this(null, message);
  }

  public String getCode() {
    return code;
  }

  public String getErrorMessage() {
    return errorMessage;
  }

  private static String buildMessage(final String code, final String message) {
    if (code == null || code.isBlank()) {
      return message;
    }
    return "[" + code + "] " + message;
  }
}
