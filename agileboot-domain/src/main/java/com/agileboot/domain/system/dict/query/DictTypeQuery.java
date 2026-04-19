package com.agileboot.domain.system.dict.query;

import cn.hutool.core.util.StrUtil;
import com.agileboot.common.core.page.AbstractPageQuery;
import com.agileboot.domain.system.dict.db.SysDictTypeEntity;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

/**
 * 字典类型查询参数
 * @author valarchie
 */
@EqualsAndHashCode(callSuper = true)
@Data
@NoArgsConstructor
@Schema(name = "字典类型查询参数")
public class DictTypeQuery extends AbstractPageQuery<SysDictTypeEntity> {

    @Schema(description = "字典名称")
    private String dictName;

    @Schema(description = "字典类型")
    private String dictType;

    @Schema(description = "状态")
    private Integer status;

    @Override
    public QueryWrapper<SysDictTypeEntity> addQueryCondition() {
        this.timeRangeColumn = "create_time";
        return new QueryWrapper<SysDictTypeEntity>()
            .like(StrUtil.isNotEmpty(dictName), "dict_name", dictName)
            .like(StrUtil.isNotEmpty(dictType), "dict_type", dictType)
            .eq(status != null, "status", status);
    }
}
