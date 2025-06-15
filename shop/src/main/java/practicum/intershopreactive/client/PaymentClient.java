package practicum.intershopreactive.client;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.oauth2.client.OAuth2AuthorizeRequest;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClient;
import org.springframework.security.oauth2.client.ReactiveOAuth2AuthorizedClientManager;
import org.springframework.security.oauth2.core.OAuth2AccessToken;
import org.springframework.stereotype.Component;
import practicum.intershopreactive.api.PaymentApi;
import practicum.intershopreactive.model.BalanceResponse;
import practicum.intershopreactive.model.PaymentRequest;
import practicum.intershopreactive.model.PaymentResponse;
import reactor.core.publisher.Mono;

@Component
@RequiredArgsConstructor
public class PaymentClient extends PaymentApi {
    private final ReactiveOAuth2AuthorizedClientManager oauthClientManager;

    @Value("${payment.server-url}")
    private String basePath;

    @PostConstruct
    public void init() {
        this.getApiClient().setBasePath(basePath);
    }

    @Override
    public Mono<PaymentResponse> processPayment(PaymentRequest paymentRequest) {
        OAuth2AuthorizeRequest authorizeRequest = OAuth2AuthorizeRequest
                .withClientRegistrationId("keycloak")
                .principal("system")
                .build();

        return oauthClientManager.authorize(authorizeRequest)
                .switchIfEmpty(Mono.error(new IllegalStateException("Could not authorize client_credentials")))
                .map(OAuth2AuthorizedClient::getAccessToken)
                .map(OAuth2AccessToken::getTokenValue)
                .flatMap(accessToken -> {
                    this.getApiClient().setBearerToken(accessToken);
                    return super.processPayment(paymentRequest);
                });
    }
}
