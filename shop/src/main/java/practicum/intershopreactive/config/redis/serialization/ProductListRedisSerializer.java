package practicum.intershopreactive.config.redis.serialization;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.serializer.RedisSerializer;
import org.springframework.data.redis.serializer.SerializationException;
import practicum.intershopreactive.entity.Product;

import java.util.List;

@RequiredArgsConstructor
public class ProductListRedisSerializer implements RedisSerializer<List<Product>> {

    private final ObjectMapper objectMapper;

    @Override
    public byte[] serialize(List<Product> items) throws SerializationException {
        try {
            if (items == null) {
                return new byte[0];
            }
            return objectMapper.writeValueAsBytes(items);
        } catch (Exception e) {
            throw new SerializationException("Could not deserialize into a list of Products", e);
        }
    }

    @Override
    public List<Product> deserialize(byte[] bytes) throws SerializationException {
        try {
            if (bytes == null || bytes.length == 0) {
                return null;
            }
            return objectMapper.readValue(bytes, new TypeReference<List<Product>>() {});
        } catch (Exception e) {
            throw new SerializationException("Could not deserialize into a list of Products", e);
        }
    }
}
