package backend.daangnbasedbackend.global.adapter.storage;

import backend.daangnbasedbackend.global.exception.common.GlobalErrorType;
import backend.daangnbasedbackend.global.exception.common.GlobalException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.test.util.ReflectionTestUtils;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class LocalStorageAdapterTest {

    @TempDir
    Path tempDir;

    private LocalStorageAdapter adapter;

    private static final String BASE_URL = "http://localhost:8080/files";

    @BeforeEach
    void setUp() {
        adapter = new LocalStorageAdapter();
        ReflectionTestUtils.setField(adapter, "basePath", tempDir.toString());
        ReflectionTestUtils.setField(adapter, "baseUrl", BASE_URL);
    }

    // ==========================================
    // upload
    // ==========================================

    @Test
    @DisplayName("업로드: 파일이 디스크에 저장되고 접근 가능한 URL을 반환한다")
    void upload_savesFileAndReturnsUrl() {
        // given
        byte[] content = "테스트 파일 내용".getBytes();

        // when
        String url = adapter.upload("images", "photo.jpg", content, "image/jpeg");

        // then
        assertThat(url).startsWith(BASE_URL + "/images/");
        assertThat(url).endsWith("_photo.jpg");

        String filename = url.replace(BASE_URL + "/images/", "");
        assertThat(tempDir.resolve("images").resolve(filename)).exists();
        assertThat(tempDir.resolve("images").resolve(filename)).hasContent("테스트 파일 내용");
    }

    @Test
    @DisplayName("업로드: 디렉토리가 없을 때 자동으로 생성한다")
    void upload_createsDirectoryAutomatically() {
        // given
        byte[] content = "내용".getBytes();

        // when
        adapter.upload("a/b/c", "file.txt", content, "text/plain");

        // then
        assertThat(tempDir.resolve("a/b/c")).isDirectory();
    }

    @Test
    @DisplayName("업로드: 같은 파일명으로 두 번 업로드하면 서로 다른 URL이 생성된다")
    void upload_sameFilename_generatesDifferentUrls() {
        // given
        byte[] content = "내용".getBytes();

        // when
        String url1 = adapter.upload("images", "same.jpg", content, "image/jpeg");
        String url2 = adapter.upload("images", "same.jpg", content, "image/jpeg");

        // then
        assertThat(url1).isNotEqualTo(url2);
    }

    @Test
    @DisplayName("업로드: IOException 발생 시 STORAGE_UPLOAD_FAILED 예외를 던진다")
    void upload_ioException_throwsStorageUploadFailed() throws IOException {
        // given: basePath 위치에 파일이 있으면 하위 디렉토리 생성 불가 → IOException
        Path blockingFile = Files.createFile(tempDir.resolve("blocking"));
        ReflectionTestUtils.setField(adapter, "basePath", blockingFile.toString());

        // when & then
        assertThatThrownBy(() -> adapter.upload("dir", "file.txt", new byte[]{}, "text/plain"))
                .isInstanceOf(GlobalException.class)
                .extracting("errorType")
                .isEqualTo(GlobalErrorType.STORAGE_UPLOAD_FAILED);
    }

    // ==========================================
    // delete
    // ==========================================

    @Test
    @DisplayName("삭제: 업로드된 파일을 정상적으로 삭제한다")
    void delete_removesUploadedFile() {
        // given
        String url = adapter.upload("images", "delete-me.jpg", "content".getBytes(), "image/jpeg");
        String filename = url.replace(BASE_URL + "/images/", "");
        Path uploadedFile = tempDir.resolve("images").resolve(filename);
        assertThat(uploadedFile).exists();

        // when
        adapter.delete(url);

        // then
        assertThat(uploadedFile).doesNotExist();
    }

    @Test
    @DisplayName("삭제: 존재하지 않는 파일 삭제 시 예외를 던지지 않는다")
    void delete_nonExistentFile_doesNotThrow() {
        // given
        String nonExistentUrl = BASE_URL + "/images/nonexistent_file.jpg";

        // when & then
        assertThatCode(() -> adapter.delete(nonExistentUrl))
                .doesNotThrowAnyException();
    }

    @Test
    @DisplayName("삭제: IOException 발생 시 STORAGE_DELETE_FAILED 예외를 던진다")
    void delete_ioException_throwsStorageDeleteFailed() throws IOException {
        // given: 비어있지 않은 디렉토리를 삭제 대상으로 지정 → DirectoryNotEmptyException
        Path dir = Files.createDirectory(tempDir.resolve("images"));
        Files.createFile(dir.resolve("inner.txt"));
        // relativePath = "images" → tempDir/images
        String dirUrl = BASE_URL + "/images";

        // when & then
        assertThatThrownBy(() -> adapter.delete(dirUrl))
                .isInstanceOf(GlobalException.class)
                .extracting("errorType")
                .isEqualTo(GlobalErrorType.STORAGE_DELETE_FAILED);
    }
}
