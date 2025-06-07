package practicum.intershopreactive.client;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import practicum.intershopreactive.api.PaymentApi;

@Component
@RequiredArgsConstructor
public class PaymentClient extends PaymentApi {
    @Value("${payment.server-url}")
    private String basePath;

    @PostConstruct
    public void init() {
        this.getApiClient().setBasePath(basePath);
    }
}
