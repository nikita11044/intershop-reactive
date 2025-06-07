package practicum.intershopreactive.config.redis.serialization;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.serializer.RedisSerializer;
import org.springframework.data.redis.serializer.SerializationException;
import practicum.intershopreactive.entity.CartItem;

import java.util.List;

@RequiredArgsConstructor
public class CartItemsRedisSerializer implements RedisSerializer<List<CartItem>> {

    private final ObjectMapper objectMapper;

    @Override
    public byte[] serialize(List<CartItem> items) throws SerializationException {
        try {
            if (items == null) {
                return new byte[0];
            }
            return objectMapper.writeValueAsBytes(items);
        } catch (Exception e) {
            throw new SerializationException("Could not deserialize into a list of CartItems", e);
        }
    }

    @Override
    public List<CartItem> deserialize(byte[] bytes) throws SerializationException {
        try {
            if (bytes == null || bytes.length == 0) {
                return null;
            }
            return objectMapper.readValue(bytes, new TypeReference<List<CartItem>>() {});
        } catch (Exception e) {
            throw new SerializationException("Could not deserialize into a list of CartItems", e);
        }
    }
}
