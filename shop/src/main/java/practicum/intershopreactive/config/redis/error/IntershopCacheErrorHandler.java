package practicum.intershopreactive.config.redis.error;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.cache.Cache;
import org.springframework.cache.interceptor.CacheErrorHandler;
import org.springframework.stereotype.Component;


@Component
public class IntershopCacheErrorHandler implements CacheErrorHandler {
    private static final Logger logger = LogManager.getLogger(IntershopCacheErrorHandler.class);

    @Override
    public void handleCacheGetError(RuntimeException exception, Cache cache, Object key) {
        logger.warn("Redis GET failure. Error: " + exception.getMessage());
    }

    @Override
    public void handleCachePutError(RuntimeException exception, Cache cache, Object key, Object value) {
        logger.warn("Redis PUT failure. Error: " + exception.getMessage());
    }

    @Override
    public void handleCacheEvictError(RuntimeException exception, Cache cache, Object key) {
        logger.warn("Redis EVICT failure. Error: " + exception.getMessage());
    }

    @Override
    public void handleCacheClearError(RuntimeException exception, Cache cache) {
        logger.warn("Redis CLEAR failure. Error: " + exception.getMessage());
    }
}
