package practicum.intershopreactive.r2dbc;

import org.springframework.data.r2dbc.repository.R2dbcRepository;
import org.springframework.stereotype.Repository;
import practicum.intershopreactive.entity.Product;
import reactor.core.publisher.Flux;

@Repository
public interface ProductR2dbcRepository extends R2dbcRepository<Product, Long> {
    Flux<Product> findByTitleContainingIgnoreCaseOrDescriptionContainingIgnoreCase(String title, String description);

    Flux<Product> findByCountGreaterThan(int count);
}
