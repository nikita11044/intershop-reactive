package practicum.intershopreactive.service;

import lombok.RequiredArgsConstructor;
import org.springframework.security.core.context.ReactiveSecurityContextHolder;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import practicum.intershopreactive.r2dbc.UserR2dbcRepository;
import reactor.core.publisher.Mono;
import practicum.intershopreactive.entity.User;

@Service
@RequiredArgsConstructor
public class UserService {
    private final UserR2dbcRepository userRepository;

    public Mono<Long> getCurrentUserId() {
        return ReactiveSecurityContextHolder.getContext()
                .map(SecurityContext::getAuthentication)
                .filter(Authentication::isAuthenticated)
                .flatMap(auth -> {
                    boolean isGuest = auth.getAuthorities().stream()
                            .anyMatch(a -> "ROLE_GUEST".equals(a.getAuthority()));
                    if (isGuest) {
                        return Mono.just(0L);
                    }
                    String username = auth.getName();
                    return userRepository.findByName(username)
                            .map(User::getId)
                            .switchIfEmpty(Mono.error(new IllegalStateException(
                                    "User not found for username: " + username
                            )));
                })
                .switchIfEmpty(Mono.error(new IllegalStateException(
                        "No authentication context available"
                )));
    }
}
