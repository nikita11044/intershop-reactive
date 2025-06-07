package practicum.intershopreactive.client;

import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import practicum.intershopreactive.api.PaymentApi;

public class PaymentClient extends PaymentApi {
    @Value("${payment.server-url}")
    private String basePath;

    @PostConstruct
    public void init() {
        this.getApiClient().setBasePath(basePath);
    }
}
