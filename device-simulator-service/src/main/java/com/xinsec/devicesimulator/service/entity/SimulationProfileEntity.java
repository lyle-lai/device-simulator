package com.xinsec.devicesimulator.service.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 模拟画像实体类，映射到数据库的 simulation_profiles 表
 */
@Data
@TableName("simulation_profiles")
public class SimulationProfileEntity {

    /**
     * 主键ID
     */
    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 画像名称，用于UI显示和管理
     */
    private String profileName;

    /**
     * 存储完整画像配置的JSON字符串
     */
    private String profileConfig;

    /**
     * 创建时间
     */
    private LocalDateTime createdAt;

    /**
     * 最后更新时间
     */
    private LocalDateTime updatedAt;
}
