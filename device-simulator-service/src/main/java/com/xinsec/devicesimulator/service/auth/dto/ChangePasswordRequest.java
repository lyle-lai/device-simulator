package com.xinsec.devicesimulator.service.auth.dto;

import lombok.Data;

/**
 * 修改密码请求的数据传输对象。
 */
@Data
public class ChangePasswordRequest {
    private String oldPassword;
    private String newPassword;
    private String confirmPassword;
}
