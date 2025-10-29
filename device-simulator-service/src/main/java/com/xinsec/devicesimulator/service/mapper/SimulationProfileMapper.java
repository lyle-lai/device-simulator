package com.xinsec.devicesimulator.service.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.xinsec.devicesimulator.service.entity.SimulationProfileEntity;
import org.apache.ibatis.annotations.Mapper;

/**
 * 模拟画像的MyBatis-Plus Mapper接口
 * 提供了对 SimulationProfileEntity 的基本CRUD操作
 */
@Mapper
public interface SimulationProfileMapper extends BaseMapper<SimulationProfileEntity> {
    // 所有基本的CRUD操作都由BaseMapper提供
    // 如果需要自定义查询，可以在这里添加方法
}
