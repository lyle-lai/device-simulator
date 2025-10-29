package com.xinsec.devicesimulator.service.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.xinsec.devicesimulator.service.entity.RequestResponseRuleEntity;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface RequestResponseRuleMapper extends BaseMapper<RequestResponseRuleEntity> {

    @Select("SELECT * FROM request_response_rule")
    List<RequestResponseRuleEntity> findAll();
}
