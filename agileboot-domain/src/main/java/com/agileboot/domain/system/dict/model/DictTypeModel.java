package com.agileboot.domain.system.dict.model;

import cn.hutool.core.bean.BeanUtil;
import com.agileboot.common.exception.ApiException;
import com.agileboot.common.exception.error.ErrorCode;
import com.agileboot.domain.system.dict.command.AddDictTypeCommand;
import com.agileboot.domain.system.dict.command.UpdateDictTypeCommand;
import com.agileboot.domain.system.dict.db.SysDictTypeEntity;
import com.agileboot.domain.system.dict.db.SysDictTypeService;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 字典类型领域模型
 * @author valarchie
 */
@EqualsAndHashCode(callSuper = true)
@Data
public class DictTypeModel extends SysDictTypeEntity {

    private SysDictTypeService dictTypeService;

    public DictTypeModel(SysDictTypeService dictTypeService) {
        this.dictTypeService = dictTypeService;
    }

    public DictTypeModel(SysDictTypeEntity entity, SysDictTypeService dictTypeService) {
        BeanUtil.copyProperties(entity, this);
        this.dictTypeService = dictTypeService;
    }

    public void loadAddCommand(AddDictTypeCommand command) {
        BeanUtil.copyProperties(command, this);
    }

    public void loadUpdateCommand(UpdateDictTypeCommand command) {
        String oldDictType = this.getDictType();
        BeanUtil.copyProperties(command, this);
        this.setDictId(command.getDictId());
        // 如果类型发生变更，校验唯一性
        if (!oldDictType.equals(command.getDictType())) {
            checkDictTypeUnique(command.getDictId());
        }
    }

    public void checkDictTypeUnique(Long excludeDictId) {
        if (!dictTypeService.isDictTypeUnique(this.getDictType(), excludeDictId)) {
            throw new ApiException(ErrorCode.Business.DICT_TYPE_IS_NOT_UNIQUE, this.getDictType());
        }
    }
}
