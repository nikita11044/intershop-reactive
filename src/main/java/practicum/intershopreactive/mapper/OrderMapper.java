package practicum.intershopreactive.mapper;

import org.mapstruct.Mapper;
import practicum.intershopreactive.dto.order.OrderDto;
import practicum.intershopreactive.entity.Order;

@Mapper(componentModel = "spring", uses = {OrderItemMapper.class})
public interface OrderMapper {
    OrderDto toDto(Order order);
}
