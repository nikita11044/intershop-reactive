package practicum.intershopreactive.r2dbc;

import org.springframework.data.r2dbc.repository.R2dbcRepository;
import org.springframework.stereotype.Repository;
import practicum.intershopreactive.entity.Product;

@Repository
public interface OrderR2dbcRepository extends R2dbcRepository<Product, Long> {
}
