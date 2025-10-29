package com.xinsec.devicesimulator.service.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("payload_repository")
public class PayloadRepositoryEntity {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String payloadKey;

    private String groupKey;

    private String payloadType;

    private String content;

    private String description;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;
}
