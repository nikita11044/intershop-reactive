package practicum.intershopreactive.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.codec.multipart.FilePart;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import static org.mockito.Mockito.any;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class FileServiceTest {

    @Mock
    private MinioService minioService;

    @Mock
    private FilePart filePart;

    @InjectMocks
    private FileService fileService;

    @Test
    void uploadFile_ShouldReturnUrl_WhenFileIsValid() {
        String fileName = "image.png";
        long size = 2048L;
        MediaType contentType = MediaType.IMAGE_PNG;

        HttpHeaders headers = new HttpHeaders();
        headers.setContentLength(size);
        headers.setContentType(contentType);

        when(filePart.filename()).thenReturn(fileName);
        when(filePart.headers()).thenReturn(headers);
        when(filePart.content()).thenReturn(Flux.empty());
        when(minioService.uploadFile(eq(fileName), any(), eq(size), eq(contentType)))
                .thenReturn(Mono.empty());

        StepVerifier.create(fileService.uploadFile(filePart))
                .expectNext("http://localhost:9000/intershop-bucket/" + fileName)
                .verifyComplete();

        verify(minioService).uploadFile(eq(fileName), any(), eq(size), eq(contentType));
    }

    @Test
    void uploadFile_ShouldReturnEmpty_WhenFileIsNull() {
        StepVerifier.create(fileService.uploadFile(null))
                .verifyComplete();

        verifyNoInteractions(minioService);
    }

    @Test
    void uploadFile_ShouldThrowException_WhenMinioFails() {
        String fileName = "image.png";
        long size = 2048L;
        MediaType contentType = MediaType.IMAGE_PNG;

        HttpHeaders headers = new HttpHeaders();
        headers.setContentLength(size);
        headers.setContentType(contentType);

        when(filePart.filename()).thenReturn(fileName);
        when(filePart.headers()).thenReturn(headers);
        when(filePart.content()).thenReturn(Flux.empty());
        when(minioService.uploadFile(eq(fileName), any(), eq(size), eq(contentType)))
                .thenReturn(Mono.error(new RuntimeException("Minio failure")));

        StepVerifier.create(fileService.uploadFile(filePart))
                .expectErrorMatches(throwable ->
                        throwable instanceof RuntimeException &&
                                throwable.getMessage().contains("Error uploading file"))
                .verify();
    }

    @Test
    void deleteFile_ShouldCallMinioService_WhenUrlIsValid() {
        String url = "http://localhost:9000/intershop-bucket/test.png";

        when(minioService.deleteFile("test.png")).thenReturn(Mono.empty());

        StepVerifier.create(fileService.deleteFile(url))
                .verifyComplete();

        verify(minioService).deleteFile("test.png");
    }

    @Test
    void deleteFile_ShouldReturnEmpty_WhenUrlIsNull() {
        StepVerifier.create(fileService.deleteFile(null))
                .verifyComplete();

        verifyNoInteractions(minioService);
    }

    @Test
    void deleteFile_ShouldReturnEmpty_WhenUrlIsEmpty() {
        StepVerifier.create(fileService.deleteFile(""))
                .verifyComplete();

        verifyNoInteractions(minioService);
    }

    @Test
    void deleteFile_ShouldThrowException_WhenMinioFails() {
        String url = "http://localhost:9000/intershop-bucket/broken.png";

        when(minioService.deleteFile("broken.png"))
                .thenReturn(Mono.error(new RuntimeException("Deletion failed")));

        StepVerifier.create(fileService.deleteFile(url))
                .expectErrorMatches(throwable ->
                        throwable instanceof RuntimeException &&
                                throwable.getMessage().contains("Error deleting file"))
                .verify();
    }
}
