package backend.daangnbasedbackend.global.application;

public interface StoragePort {
    String upload(String directory, String originalFilename, byte[] bytes, String contentType);
    void delete(String fileUrl);
}
