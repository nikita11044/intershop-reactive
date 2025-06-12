package practicum.intershopreactive.config.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.server.SecurityWebFilterChain;

import static org.springframework.security.config.Customizer.withDefaults;

@Configuration
@EnableWebFluxSecurity
public class SecurityConfig {

    @Bean
    public SecurityWebFilterChain springSecurityFilterChain(ServerHttpSecurity http) {
        return http
                .csrf(ServerHttpSecurity.CsrfSpec::disable)
                .formLogin(withDefaults())
                .anonymous(anonymous -> anonymous
                        .principal("guest")
                        .authorities("ROLE_GUEST")
                        .key("poison_pen")
                )
                .authorizeExchange(exchanges ->
                        exchanges
                                .pathMatchers("/products/new").hasRole("ADMIN")
                                .pathMatchers(HttpMethod.POST, "/products").hasRole("ADMIN")
                                .pathMatchers("/cart").authenticated()
                                .pathMatchers("/cart/**").authenticated()
                                .pathMatchers("/orders/**").authenticated()
                                .pathMatchers("/products/**").permitAll()
                                .pathMatchers("/products").permitAll()
                                .pathMatchers("/").permitAll()
                                .anyExchange().authenticated()
                )
                .build();
    }

    @Bean
    PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
