package com.smarthire.modules.job.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.smarthire.modules.job.entity.JobEntity;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface JobMapper extends BaseMapper<JobEntity> {
}
