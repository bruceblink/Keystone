package com.agileboot.integrationTest;

import com.agileboot.domain.system.config.db.SysConfigMapper;
import com.agileboot.domain.system.config.db.SysConfigServiceImpl;
import com.agileboot.domain.system.dept.db.SysDeptMapper;
import com.agileboot.domain.system.dept.db.SysDeptServiceImpl;
import com.agileboot.domain.system.log.db.SysLoginInfoMapper;
import com.agileboot.domain.system.log.db.SysOperationLogMapper;
import com.agileboot.domain.system.menu.db.SysMenuMapper;
import com.agileboot.domain.system.menu.db.SysMenuServiceImpl;
import com.agileboot.domain.system.notice.db.SysNoticeMapper;
import com.agileboot.domain.system.post.db.SysPostMapper;
import com.agileboot.domain.system.post.db.SysPostServiceImpl;
import com.agileboot.domain.system.role.db.SysRoleMapper;
import com.agileboot.domain.system.role.db.SysRoleMenuMapper;
import com.agileboot.domain.system.role.db.SysRoleServiceImpl;
import com.agileboot.domain.system.user.db.SysUserMapper;
import com.agileboot.domain.system.user.db.SysUserServiceImpl;
import com.agileboot.infrastructure.config.ApplicationConfig;
import org.apache.ibatis.session.SqlSession;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.ComponentScan.Filter;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.FilterType;
import org.springframework.context.annotation.Import;

/**
 * 集成测试配置类
 * @author valarchie
 */
@SpringBootApplication(exclude = {DataSourceAutoConfiguration.class}, scanBasePackages = "com.agileboot")
@ComponentScan(
    basePackages = "com.agileboot",
    excludeFilters = @Filter(type = FilterType.ASSIGNABLE_TYPE, classes = ApplicationConfig.class)
)
@Import({
    SysConfigServiceImpl.class,
    SysDeptServiceImpl.class,
    SysMenuServiceImpl.class,
    SysPostServiceImpl.class,
    SysRoleServiceImpl.class,
    SysUserServiceImpl.class,
    IntegrationTestApplication.MapperBeanConfig.class
})
public class IntegrationTestApplication {
    public static void main(String[] args) {
        SpringApplication.run(IntegrationTestApplication.class, args);
    }

    @Configuration
    static class MapperBeanConfig {

        private <T> T mapper(SqlSession sqlSession, Class<T> mapperType) {
            if (!sqlSession.getConfiguration().hasMapper(mapperType)) {
                sqlSession.getConfiguration().addMapper(mapperType);
            }
            return sqlSession.getMapper(mapperType);
        }

        @Bean
        SysConfigMapper sysConfigMapper(SqlSession sqlSession) {
            return mapper(sqlSession, SysConfigMapper.class);
        }

        @Bean
        SysDeptMapper sysDeptMapper(SqlSession sqlSession) {
            return mapper(sqlSession, SysDeptMapper.class);
        }

        @Bean
        SysMenuMapper sysMenuMapper(SqlSession sqlSession) {
            return mapper(sqlSession, SysMenuMapper.class);
        }

        @Bean
        SysPostMapper sysPostMapper(SqlSession sqlSession) {
            return mapper(sqlSession, SysPostMapper.class);
        }

        @Bean
        SysRoleMapper sysRoleMapper(SqlSession sqlSession) {
            return mapper(sqlSession, SysRoleMapper.class);
        }

        @Bean
        SysRoleMenuMapper sysRoleMenuMapper(SqlSession sqlSession) {
            return mapper(sqlSession, SysRoleMenuMapper.class);
        }

        @Bean
        SysUserMapper sysUserMapper(SqlSession sqlSession) {
            return mapper(sqlSession, SysUserMapper.class);
        }

        @Bean
        SysNoticeMapper sysNoticeMapper(SqlSession sqlSession) {
            return mapper(sqlSession, SysNoticeMapper.class);
        }

        @Bean
        SysLoginInfoMapper sysLoginInfoMapper(SqlSession sqlSession) {
            return mapper(sqlSession, SysLoginInfoMapper.class);
        }

        @Bean
        SysOperationLogMapper sysOperationLogMapper(SqlSession sqlSession) {
            return mapper(sqlSession, SysOperationLogMapper.class);
        }
    }
}
