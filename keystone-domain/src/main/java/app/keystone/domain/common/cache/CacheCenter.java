package app.keystone.domain.common.cache;

import cn.hutool.extra.spring.SpringUtil;
import app.keystone.infrastructure.cache.caffeine.AbstractCaffeineCacheTemplate;
import app.keystone.infrastructure.cache.redis.RedisCacheTemplate;
import app.keystone.infrastructure.user.web.SystemLoginUser;
import app.keystone.domain.system.dept.db.SysDeptEntity;
import app.keystone.domain.system.dict.db.SysDictDataEntity;
import app.keystone.domain.system.post.db.SysPostEntity;
import app.keystone.domain.system.role.db.SysRoleEntity;
import app.keystone.domain.system.user.db.SysUserEntity;
import java.util.List;
import jakarta.annotation.PostConstruct;
import org.springframework.stereotype.Component;

/**
 * 缓存中心  提供全局访问点
 * 如果是领域类的缓存  可以自己新建一个直接放在CacheCenter   不用放在infrastructure包里的LocalCacheService
 * 或者RedisCacheService
 * @author valarchie
 */
@Component
public class CacheCenter {

    private AbstractCaffeineCacheTemplate<String> configCache;

    private AbstractCaffeineCacheTemplate<SysDeptEntity> deptCache;

    private RedisCacheTemplate<String> captchaCache;

    private RedisCacheTemplate<SystemLoginUser> loginUserCache;

    private RedisCacheTemplate<SysUserEntity> userCache;

    private RedisCacheTemplate<SysRoleEntity> roleCache;

    private RedisCacheTemplate<SysPostEntity> postCache;

    private RedisCacheTemplate<List<SysDictDataEntity>> dictDataCache;

    @PostConstruct
    public void init() {
        LocalCacheService localCache = SpringUtil.getBean(LocalCacheService.class);
        RedisCacheService redisCache = SpringUtil.getBean(RedisCacheService.class);

        this.configCache = localCache.configCache;
        this.deptCache = localCache.deptCache;

        this.captchaCache = redisCache.captchaCache;
        this.loginUserCache = redisCache.loginUserCache;
        this.userCache = redisCache.userCache;
        this.roleCache = redisCache.roleCache;
        this.postCache = redisCache.postCache;
        this.dictDataCache = redisCache.dictDataCache;
    }

    public static AbstractCaffeineCacheTemplate<String> configCache() {
        return SpringUtil.getBean(CacheCenter.class).configCache;
    }

    public static AbstractCaffeineCacheTemplate<SysDeptEntity> deptCache() {
        return SpringUtil.getBean(CacheCenter.class).deptCache;
    }

    public static RedisCacheTemplate<String> captchaCache() {
        return SpringUtil.getBean(CacheCenter.class).captchaCache;
    }

    public static RedisCacheTemplate<SystemLoginUser> loginUserCache() {
        return SpringUtil.getBean(CacheCenter.class).loginUserCache;
    }

    public static RedisCacheTemplate<SysUserEntity> userCache() {
        return SpringUtil.getBean(CacheCenter.class).userCache;
    }

    public static RedisCacheTemplate<SysRoleEntity> roleCache() {
        return SpringUtil.getBean(CacheCenter.class).roleCache;
    }

    public static RedisCacheTemplate<SysPostEntity> postCache() {
        return SpringUtil.getBean(CacheCenter.class).postCache;
    }

    public static RedisCacheTemplate<List<SysDictDataEntity>> dictDataCache() {
        return SpringUtil.getBean(CacheCenter.class).dictDataCache;
    }

}

