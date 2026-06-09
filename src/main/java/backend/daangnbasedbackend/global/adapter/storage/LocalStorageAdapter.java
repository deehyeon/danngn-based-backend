package backend.daangnbasedbackend.global.adapter.storage;

import backend.daangnbasedbackend.global.application.dto.PresignedUrlRes;
import backend.daangnbasedbackend.global.application.provided.StoragePort;
import backend.daangnbasedbackend.global.exception.common.GlobalErrorType;
import backend.daangnbasedbackend.global.exception.common.GlobalException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.UUID;

@Slf4j
@Component
@ConditionalOnProperty(name = "storage.type", havingValue = "local", matchIfMissing = true)
public class LocalStorageAdapter implements StoragePort {

    @Value("${storage.local.base-path:./uploads}")
    private String basePath;

    @Value("${storage.local.base-url:http://localhost:8080/files}")
    private String baseUrl;

    @Override
    public String upload(String directory, String originalFilename, byte[] bytes, String contentType) {
        try {
            String uniqueFilename = UUID.randomUUID() + "_" + originalFilename;
            Path dirPath = Paths.get(basePath, directory);
            Files.createDirectories(dirPath);
            Files.write(dirPath.resolve(uniqueFilename), bytes);
            return baseUrl + "/" + directory + "/" + uniqueFilename;
        } catch (IOException e) {
            log.error("로컬 파일 업로드 실패: directory={}, filename={}", directory, originalFilename, e);
            throw new GlobalException(GlobalErrorType.STORAGE_UPLOAD_FAILED);
        }
    }

    @Override
    public void delete(String fileUrl) {
        try {
            String relativePath = fileUrl.replace(baseUrl + "/", "");
            Path filePath = Paths.get(basePath, relativePath);
            Files.deleteIfExists(filePath);
        } catch (IOException e) {
            log.error("로컬 파일 삭제 실패: fileUrl={}", fileUrl, e);
            throw new GlobalException(GlobalErrorType.STORAGE_DELETE_FAILED);
        }
    }

    @Override
    public PresignedUrlRes generatePresignedUrl(String directory, String originalFilename, String contentType) {
        throw new UnsupportedOperationException(
                "로컬 스토리지는 Presigned URL을 지원하지 않습니다. POST /v1/storage/upload 엔드포인트를 사용하세요.");
    }
}
