package app.keystone.infrastructure.cache.aop;

import com.github.benmanes.caffeine.cache.Caffeine;
import java.util.Objects;
import java.util.concurrent.Callable;
import java.util.concurrent.TimeUnit;
import jakarta.annotation.PostConstruct;
import org.springframework.cache.Cache;
import org.springframework.cache.support.SimpleValueWrapper;
/**
 * @author likanug
 */
//@Component
public class CaffeineCacheBean implements Cache {

    /**
     * 缓存仓库
     */
    private com.github.benmanes.caffeine.cache.Cache<Object, Object> storage;

    @PostConstruct
    private void init() {
        // Caffeine 的普通 Cache 不支持 refreshAfterWrite（需要 LoadingCache），改用 expireAfterWrite
        storage = Caffeine.newBuilder()
            // 设置缓存的容量为100
            .maximumSize(100)
            // 设置初始容量为16
            .initialCapacity(16)
            // 设置过期时间为写入缓存后10分钟过期
            .expireAfterWrite(10, TimeUnit.MINUTES)
            .build();
    }

    @Override
    public String getName() {
        return CacheNameConstants.CAFFEINE;
    }



    @Override
    public ValueWrapper get(Object key) {
        if (Objects.isNull(key)) {
            return null;
        }
        Object ifPresent = storage.getIfPresent(key.toString());
        return Objects.isNull(ifPresent) ? null : new SimpleValueWrapper(ifPresent);
    }

    @Override
    public void put(Object key, Object value) {
        if (key == null || key.toString().isEmpty()) {
            return;
        }
        storage.put(key, value);
    }

    @Override
    public void evict(Object key) {
        if (key == null) {
            return;
        }

        storage.invalidate(key);
    }



    /*-----------------------暂时不用实现的方法-----------------*/


    @Override
    public <T> T get(Object key, Class<T> type) {
        return null;
    }

    @Override
    public <T> T get(Object key, Callable<T> valueLoader) {
        return null;
    }

    @Override
    public void clear() {

    }

    @Override
    public Object getNativeCache() {
        return this;
    }
}
