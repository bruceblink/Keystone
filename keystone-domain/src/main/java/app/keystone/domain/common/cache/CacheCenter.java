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

    public static AbstractCaffeineCacheTemplate<String> configCache;

    public static AbstractCaffeineCacheTemplate<SysDeptEntity> deptCache;

    public static RedisCacheTemplate<String> captchaCache;

    public static RedisCacheTemplate<SystemLoginUser> loginUserCache;

    public static RedisCacheTemplate<SysUserEntity> userCache;

    public static RedisCacheTemplate<SysRoleEntity> roleCache;

    public static RedisCacheTemplate<SysPostEntity> postCache;

    public static RedisCacheTemplate<List<SysDictDataEntity>> dictDataCache;

    @PostConstruct
    public void init() {
        LocalCacheService localCache = SpringUtil.getBean(LocalCacheService.class);
        RedisCacheService redisCache = SpringUtil.getBean(RedisCacheService.class);

        configCache = localCache.configCache;
        deptCache = localCache.deptCache;

        captchaCache = redisCache.captchaCache;
        loginUserCache = redisCache.loginUserCache;
        userCache = redisCache.userCache;
        roleCache = redisCache.roleCache;
        postCache = redisCache.postCache;
        dictDataCache = redisCache.dictDataCache;
    }

}
