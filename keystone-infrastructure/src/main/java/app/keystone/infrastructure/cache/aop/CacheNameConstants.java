package app.keystone.infrastructure.cache.aop;

/**
 * @author valarchie
 */
public class CacheNameConstants {

    public static final String CAFFEINE = "caffeine";

    /** @deprecated 已迁移至 Caffeine，请使用 {@link #CAFFEINE} */
    @Deprecated
    public static final String GUAVA = "guava";

    public static final String REDIS = "redis";

    private CacheNameConstants() {
    }
}
