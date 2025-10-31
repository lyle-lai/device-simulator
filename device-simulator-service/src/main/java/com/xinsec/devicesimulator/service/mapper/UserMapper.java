package com.xinsec.devicesimulator.service.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.xinsec.devicesimulator.service.entity.UserEntity;
import org.apache.ibatis.annotations.Mapper;

/**
 * 用户数据访问接口
 */
@Mapper
public interface UserMapper extends BaseMapper<UserEntity> {
}
