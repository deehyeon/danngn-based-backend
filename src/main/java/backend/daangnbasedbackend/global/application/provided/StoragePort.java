package backend.daangnbasedbackend.global.application.provided;

import backend.daangnbasedbackend.global.application.dto.PresignedUrlRes;

public interface StoragePort {
    String upload(String directory, String originalFilename, byte[] bytes, String contentType);
    void delete(String fileUrl);
    PresignedUrlRes generatePresignedUrl(String directory, String originalFilename, String contentType);
}
