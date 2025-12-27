# file-depot-client

File Depot 서비스를 위한 Java 클라이언트 라이브러리입니다.

## 설치

### Gradle (JitPack)

```gradle
repositories {
    maven { url 'https://jitpack.io' }
}

dependencies {
    implementation 'com.github.agent-hanju:file-depot-client:0.1.0'
}
```

### Maven (JitPack)

```xml
<repositories>
    <repository>
        <id>jitpack.io</id>
        <url>https://jitpack.io</url>
    </repository>
</repositories>

<dependency>
    <groupId>com.github.agent-hanju</groupId>
    <artifactId>file-depot-client</artifactId>
    <version>0.1.0</version>
</dependency>
```

## 사용법

### 클라이언트 생성

```java
// 기본 생성
FileDepotClient client = new HttpFileDepotClient("http://localhost:8080");

// WebClient.Builder 사용 (타임아웃 등 커스텀 설정)
WebClient.Builder builder = WebClient.builder()
    .clientConnector(new ReactorClientHttpConnector(
        HttpClient.create().responseTimeout(Duration.ofSeconds(30))
    ));
FileDepotClient client = new HttpFileDepotClient(builder, "http://localhost:8080");
```

### 파일 업로드

```java
// 1. 업로드 URL 발급
UploadUrlResponse uploadInfo = client.prepareUpload();
String fileId = uploadInfo.id();
String uploadUrl = uploadInfo.uploadUrl();

// 2. presigned URL로 파일 업로드 (HTTP PUT)
// ... 직접 HTTP 클라이언트로 업로드 ...

// 3. 업로드 완료 확인
StorageItemDto metadata = client.confirmUpload(fileId);
```

### 파일 다운로드

```java
// 다운로드 URL 발급
DownloadUrlResponse downloadInfo = client.getDownloadUrl(fileId);
String downloadUrl = downloadInfo.downloadUrl();

// presigned URL로 파일 다운로드
// ... 직접 HTTP 클라이언트로 다운로드 ...
```

### 파일 메타데이터 조회

```java
StorageItemDto metadata = client.getFileMetadata(fileId);
```

### 파일 삭제

```java
client.deleteFiles(List.of(fileId1, fileId2));
```

### 일괄 다운로드 (ZIP)

```java
byte[] zipBytes = client.downloadBatch(List.of(fileId1, fileId2));
```

## 예외 처리

| 예외                       | 설명                                       |
| -------------------------- | ------------------------------------------ |
| `IllegalArgumentException` | 잘못된 파라미터 (null, blank, empty 등)    |
| `FileDepotException`       | 서버에서 반환한 에러 (success=false)       |
| `FileDepotClientException` | 클라이언트 측 에러 (네트워크, 타임아웃 등) |

```java
try {
    client.getFileMetadata(fileId);
} catch (IllegalArgumentException e) {
    // 파라미터 오류
} catch (FileDepotException e) {
    // 서버 오류 (파일 없음 등)
} catch (FileDepotClientException e) {
    // 네트워크 오류
}
```

## 연관 프로젝트

- [file-depot](https://github.com/kimdoyeon-goryeong23rd/file-depot) - File Depot 서버
- [file-depot-api](https://github.com/agent-hanju/file-depot-api) - API DTO 라이브러리

## 라이선스

Saltlux Internal License - [LICENSE](LICENSE) 참조
