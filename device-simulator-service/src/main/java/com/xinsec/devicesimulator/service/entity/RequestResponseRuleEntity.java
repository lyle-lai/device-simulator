package com.xinsec.devicesimulator.service.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("request_response_rule")
public class RequestResponseRuleEntity {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String ruleGroup;

    private String requestKey;

    private String responseKey;

    private String description;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;

    public String getRequestKeyword() {
        return requestKey;
    }

    public String getResponsePayload() {
        return responseKey;
    }
}
