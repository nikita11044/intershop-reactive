package practicum.intershopreactive.client;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import practicum.intershopreactive.api.BalanceApi;

@Component
@RequiredArgsConstructor
public class BalanceClient extends BalanceApi {
    @Value("${payment.server-url}")
    private String basePath;

    @PostConstruct
    public void init() {
        this.getApiClient().setBasePath(basePath);
    }
}
