package practicum.intershopreactive.r2dbc;

import org.springframework.data.r2dbc.repository.R2dbcRepository;
import org.springframework.stereotype.Repository;
import practicum.intershopreactive.entity.User;
import reactor.core.publisher.Mono;

@Repository
public interface UserR2dbcRepository extends R2dbcRepository<User, Long> {
    Mono<User> findByName(String name);
}
