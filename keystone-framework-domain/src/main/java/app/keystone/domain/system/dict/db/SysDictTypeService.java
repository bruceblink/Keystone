package app.keystone.domain.system.dict.db;

import com.baomidou.mybatisplus.spring.service.IService;

/**
 * 字典类型 服务接口
 * @author likanug
 */
public interface SysDictTypeService extends IService<SysDictTypeEntity> {

    boolean isDictTypeUnique(String dictType, Long excludeDictId);
}
