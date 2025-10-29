package com.xinsec.devicesimulator.service.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.xinsec.devicesimulator.service.entity.PayloadRepositoryEntity;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface PayloadRepositoryMapper extends BaseMapper<PayloadRepositoryEntity> {

    @Select("SELECT * FROM payload_repository WHERE payload_key = #{payloadKey}")
    PayloadRepositoryEntity findByName(String payloadKey);

    @Select("SELECT * FROM payload_repository WHERE group_key = #{groupKey}")
    List<PayloadRepositoryEntity> findByGroupKey(String groupKey);
}
