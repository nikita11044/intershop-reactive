package practicum.intershopreactive.r2dbc;

import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.r2dbc.repository.R2dbcRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import practicum.intershopreactive.entity.Product;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Repository
public interface ProductR2dbcRepository extends R2dbcRepository<Product, Long> {
    @Query("SELECT * FROM products WHERE (:search IS NULL OR title ILIKE :searchPattern OR description ILIKE :searchPattern) ORDER BY " +
            "CASE WHEN :sort = 'ALPHA' THEN title::text " +
            "     WHEN :sort = 'PRICE' THEN price::text " +
            "     ELSE id::text END " +
            "LIMIT :limit OFFSET :offset")
    Flux<Product> findProducts(@Param("search") String search,
                               @Param("searchPattern") String searchPattern,
                               @Param("sort") String sort,
                               @Param("limit") int limit,
                               @Param("offset") int offset);

    @Query("SELECT COUNT(*) FROM products WHERE (:search IS NULL OR title ILIKE :searchPattern OR description ILIKE :searchPattern)")
    Mono<Long> countProducts(@Param("search") String search,
                             @Param("searchPattern") String searchPattern);

}
