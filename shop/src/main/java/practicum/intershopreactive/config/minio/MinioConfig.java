package practicum.intershopreactive.config.minio;

import io.minio.MinioClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties(prefix = "minio")
public class MinioConfig {

    @Value("${intershop.file-storage.endpoint}")
    private String url;

    @Value("${intershop.file-storage.username}")
    private String accessKey;

    @Value("${intershop.file-storage.password}")
    private String secretKey;

    @Bean
    public MinioClient minioClient() {
        return MinioClient.builder()
                .endpoint(url)
                .credentials(accessKey, secretKey)
                .build();
    }
}
