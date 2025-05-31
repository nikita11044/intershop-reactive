package practicum.intershopreactive.service;

import io.minio.MinioClient;
import io.minio.PutObjectArgs;
import io.minio.RemoveObjectArgs;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.DataBufferUtils;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.io.FileInputStream;
import java.io.InputStream;
import java.nio.file.Files;

@Service
public class MinioService {
    private final MinioClient minioClient;
    private final String bucketName = "intershop-bucket";

    public MinioService(MinioClient minioClient) {
        this.minioClient = minioClient;
    }

    public Mono<Void> uploadFile(String fileName, Flux<DataBuffer> dataBufferFlux, long contentLength, MediaType contentType) {
        long partSize = contentLength > 0 ? contentLength : 5 * 1024 * 1024;

        return Mono.fromCallable(() -> Files.createTempFile("upload-", fileName))
                .flatMap(tempFile ->
                        DataBufferUtils.write(dataBufferFlux, tempFile)
                                .then(Mono.fromCallable(() -> {
                                    try (InputStream inputStream = new FileInputStream(tempFile.toFile())) {
                                        minioClient.putObject(
                                                PutObjectArgs.builder()
                                                        .bucket(bucketName)
                                                        .object(fileName)
                                                        .stream(inputStream, contentLength, partSize)
                                                        .contentType(contentType != null ? contentType.toString() : "application/octet-stream")
                                                        .build()
                                        );
                                    }
                                    return tempFile;
                                }))
                                .publishOn(Schedulers.boundedElastic())
                                .doFinally(signal -> {
                                    try {
                                        Files.deleteIfExists(tempFile);
                                    } catch (Exception e) {
                                        System.out.println(e.getMessage());
                                    }
                                })
                )
                .subscribeOn(Schedulers.boundedElastic())
                .then();
    }

    public Mono<Void> deleteFile(String fileName) {
        return Mono.fromRunnable(() -> {
            try {
                minioClient.removeObject(
                        RemoveObjectArgs.builder()
                                .bucket(bucketName)
                                .object(fileName)
                                .build()
                );
            } catch (Exception e) {
                throw new RuntimeException("Error deleting file", e);
            }
        }).subscribeOn(Schedulers.boundedElastic()).then();
    }
}
