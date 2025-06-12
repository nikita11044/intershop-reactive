package practicum.intershopreactive.service.security;

import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.ReactiveUserDetailsService;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import practicum.intershopreactive.r2dbc.UserR2dbcRepository;
import reactor.core.publisher.Mono;

@Service
@RequiredArgsConstructor
public class IntershopUserDetailsService implements ReactiveUserDetailsService {

    private final UserR2dbcRepository userRepository;

    @Override
    public Mono<UserDetails> findByUsername(String username) {
        return userRepository
                .findByName(username)
                .map(user -> User
                        .withUsername(user.getName())
                        .password(user.getPassword())
                        .roles(user.getRole())
                        .build()
                )
                .switchIfEmpty(
                        Mono.error(
                                new UsernameNotFoundException("User not found")
                        )
                );
    }
}
