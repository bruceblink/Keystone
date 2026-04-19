package com.agileboot.domain.system.dict.db;

import com.baomidou.mybatisplus.extension.service.IService;

/**
 * 字典类型 服务接口
 * @author valarchie
 */
public interface SysDictTypeService extends IService<SysDictTypeEntity> {

    boolean isDictTypeUnique(String dictType, Long excludeDictId);
}
