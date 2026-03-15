package com.smarthire.modules.operationlog.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.smarthire.modules.operationlog.entity.OperationLogEntity;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface OperationLogMapper extends BaseMapper<OperationLogEntity> {
}
