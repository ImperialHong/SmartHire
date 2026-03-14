package com.smarthire.modules.application.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.smarthire.modules.application.entity.ApplicationEntity;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface ApplicationMapper extends BaseMapper<ApplicationEntity> {
}
