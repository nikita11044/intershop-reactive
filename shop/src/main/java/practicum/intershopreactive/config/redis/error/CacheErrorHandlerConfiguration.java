package practicum.intershopreactive.config.redis.error;

import org.springframework.cache.annotation.CachingConfigurer;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.interceptor.CacheErrorHandler;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableCaching
public class CacheErrorHandlerConfiguration implements CachingConfigurer {

    @Override
    public CacheErrorHandler errorHandler() {
        return new IntershopCacheErrorHandler();
    }
}
