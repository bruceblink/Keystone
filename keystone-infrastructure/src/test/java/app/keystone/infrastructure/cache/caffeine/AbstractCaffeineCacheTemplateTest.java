package app.keystone.infrastructure.cache.caffeine;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * {@link AbstractCaffeineCacheTemplate} 单元测试
 */
@DisplayName("AbstractCaffeineCacheTemplate 单元测试")
class AbstractCaffeineCacheTemplateTest {

    /** 数据库调用计数器，用于验证缓存是否回避了重复 DB 调用 */
    private AtomicInteger dbCallCount;

    /** 被测缓存实例 */
    private AbstractCaffeineCacheTemplate<String> cache;

    @BeforeEach
    void setUp() {
        dbCallCount = new AtomicInteger(0);
        cache = new AbstractCaffeineCacheTemplate<String>() {
            @Override
            public String getObjectFromDb(Object id) {
                dbCallCount.incrementAndGet();
                if ("missing".equals(id)) {
                    return null;
                }
                return "value_" + id;
            }
        };
    }

    @Test
    @DisplayName("缓存未命中时应回调 getObjectFromDb 并返回正确值")
    void get_shouldLoadFromDbOnCacheMiss() {
        String result = cache.get("key1");

        assertThat(result).isEqualTo("value_key1");
        assertThat(dbCallCount.get()).isEqualTo(1);
    }

    @Test
    @DisplayName("缓存命中时不应重复调用 getObjectFromDb")
    void get_shouldReturnCachedValueWithoutDbCall() {
        cache.get("key1"); // 第一次，触发 DB 加载
        String result = cache.get("key1"); // 第二次，应命中缓存

        assertThat(result).isEqualTo("value_key1");
        assertThat(dbCallCount.get()).isEqualTo(1);
    }

    @Test
    @DisplayName("key 为 null 时应返回 null 且不触发 DB 调用")
    void get_withNullKey_shouldReturnNull() {
        String result = cache.get(null);

        assertThat(result).isNull();
        assertThat(dbCallCount.get()).isEqualTo(0);
    }

    @Test
    @DisplayName("DB 中不存在对应数据时应返回 null")
    void get_whenDbReturnsNull_shouldReturnNull() {
        String result = cache.get("missing");

        assertThat(result).isNull();
    }

    @Test
    @DisplayName("invalidate 后再次 get 应重新调用 DB")
    void invalidate_shouldEvictCacheAndTriggerReload() {
        cache.get("key1"); // 加载到缓存
        assertThat(dbCallCount.get()).isEqualTo(1);

        cache.invalidate("key1"); // 驱逐
        cache.get("key1"); // 重新加载

        assertThat(dbCallCount.get()).isEqualTo(2);
    }

    @Test
    @DisplayName("invalidate 传入空字符串时不应抛出异常")
    void invalidate_withEmptyKey_shouldNotThrow() {
        // 不应抛出任何异常
        cache.invalidate("");
        cache.invalidate(null);
    }

    @Test
    @DisplayName("invalidateAll 后所有 key 应重新从 DB 加载")
    void invalidateAll_shouldEvictAllCachedEntries() {
        cache.get("key1");
        cache.get("key2");
        assertThat(dbCallCount.get()).isEqualTo(2);

        cache.invalidateAll();

        cache.get("key1");
        cache.get("key2");
        assertThat(dbCallCount.get()).isEqualTo(4);
    }

    @Test
    @DisplayName("不同 key 应独立缓存，互不影响")
    void get_differentKeys_shouldCacheIndependently() {
        String v1 = cache.get("k1");
        String v2 = cache.get("k2");

        assertThat(v1).isEqualTo("value_k1");
        assertThat(v2).isEqualTo("value_k2");
        assertThat(dbCallCount.get()).isEqualTo(2);

        // 再次 get 两个 key，均应命中缓存
        cache.get("k1");
        cache.get("k2");
        assertThat(dbCallCount.get()).isEqualTo(2);
    }
}
