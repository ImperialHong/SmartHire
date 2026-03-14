package com.smarthire.modules.auth.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.smarthire.modules.auth.entity.UserEntity;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface UserMapper extends BaseMapper<UserEntity> {
}
