package practicum.intershopreactive.service;

import lombok.RequiredArgsConstructor;
import org.springframework.http.codec.multipart.FilePart;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import reactor.core.publisher.Mono;

import java.io.InputStream;

@Service
@RequiredArgsConstructor
public class FileService {
    private final MinioService minioService;
    private static final String BUCKET_NAME = "intershop-bucket";
    private static final String BASE_URL = "http://intershop-minio:9000/" + BUCKET_NAME + "/";

    public Mono<String> uploadFile(FilePart filePart) {
        if (filePart == null) {
            return Mono.empty();
        }

        String fileName = filePart.filename();

        return minioService.uploadFile(fileName, filePart.content(), filePart.headers().getContentLength(), filePart.headers().getContentType())
                .then(Mono.just(BASE_URL + fileName))
                .onErrorMap(e -> new RuntimeException("Error uploading file", e));
    }

    public Mono<Void> deleteFile(String fileUrl) {
        if (fileUrl == null || fileUrl.isEmpty()) {
            return Mono.empty();
        }
        String fileName = extractFilenameFromUrl(fileUrl);
        return minioService.deleteFile(fileName)
                .onErrorMap(e -> new RuntimeException("Error deleting file", e));
    }

    private String extractFilenameFromUrl(String fileUrl) {
        return fileUrl.substring(fileUrl.lastIndexOf('/') + 1);
    }
}
