package practicum.intershopreactive.r2dbc;

import org.springframework.data.r2dbc.repository.R2dbcRepository;
import org.springframework.stereotype.Repository;
import practicum.intershopreactive.entity.OrderItem;
import reactor.core.publisher.Flux;

@Repository
public interface OrderItemR2dbcRepository extends R2dbcRepository<OrderItem, Long> {
    Flux<OrderItem> findByOrderId(Long orderId);
}
