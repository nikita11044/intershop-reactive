package practicum.intershopreactive.mapper;

import org.mapstruct.Mapper;
import practicum.intershopreactive.dto.order.OrderItemDto;
import practicum.intershopreactive.entity.OrderItem;

@Mapper(componentModel = "spring")
public interface OrderItemMapper {
    OrderItemDto toDto(OrderItem orderItem);
}

