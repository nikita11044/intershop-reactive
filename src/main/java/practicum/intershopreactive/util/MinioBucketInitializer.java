package practicum.intershopreactive.util;

import io.minio.MakeBucketArgs;
import io.minio.MinioClient;
import io.minio.SetBucketPolicyArgs;
import io.minio.errors.MinioException;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;

@Component
public class MinioBucketInitializer {
    private final MinioClient minioClient;
    private final String bucketName = "intershop-bucket";

    public MinioBucketInitializer(MinioClient minioClient) {
        this.minioClient = minioClient;
        createBucketIfNotExists();
    }

    private void createBucketIfNotExists() {
        try {
            boolean bucketExists = minioClient.bucketExists(io.minio.BucketExistsArgs.builder().bucket(bucketName).build());
            if (!bucketExists) {
                minioClient.makeBucket(MakeBucketArgs.builder().bucket(bucketName).build());
                setPublicBucketPolicy();
            }
        } catch (MinioException | IllegalArgumentException | InvalidKeyException | IOException |
                 NoSuchAlgorithmException e) {
            throw new RuntimeException("Error creating or configuring bucket: " + e.getMessage(), e);
        }
    }

    private void setPublicBucketPolicy() {
        try {
            String policy = "{\n" +
                    "  \"Version\": \"2012-10-17\",\n" +
                    "  \"Statement\": [\n" +
                    "    {\n" +
                    "      \"Effect\": \"Allow\",\n" +
                    "      \"Principal\": \"*\",\n" +
                    "      \"Action\": \"s3:GetObject\",\n" +
                    "      \"Resource\": \"arn:aws:s3:::" + bucketName + "/*\"\n" +
                    "    }\n" +
                    "  ]\n" +
                    "}";

            minioClient.setBucketPolicy(
                    SetBucketPolicyArgs.builder()
                            .bucket(bucketName)
                            .config(policy)
                            .build()
            );
        } catch (MinioException | IllegalArgumentException | InvalidKeyException | IOException |
                 NoSuchAlgorithmException e) {
            throw new RuntimeException("Error setting public access policy for the bucket: " + e.getMessage(), e);
        }
    }
}


