package com.xinsec.devicesimulator.service.controller;

import com.xinsec.devicesimulator.service.auth.UserService;
import com.xinsec.devicesimulator.service.auth.dto.ChangePasswordRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.security.Principal;

/**
 * 用户相关操作的控制器。
 */
@RestController
@RequestMapping("/api/user")
@RequiredArgsConstructor
@Slf4j
public class UserController {

    private final UserService userService;

    /**
     * 修改当前登录用户的密码。
     * @param request 包含旧密码和新密码的请求体。
     * @param principal Spring Security提供的当前用户信息。
     * @return 成功或失败的HTTP响应。
     */
    @PostMapping("/change-password")
    public ResponseEntity<?> changePassword(@RequestBody ChangePasswordRequest request, Principal principal) {
        // 校验新密码和确认密码是否一致
        if (request.getNewPassword() == null || !request.getNewPassword().equals(request.getConfirmPassword())) {
            return ResponseEntity.badRequest().body("新密码和确认密码不一致。");
        }

        // 校验新密码长度等规则（示例：至少6位）
        if (request.getNewPassword().length() < 6) {
            return ResponseEntity.badRequest().body("新密码长度不能少于6位。");
        }

        try {
            // 从 Principal 对象中获取当前用户名
            String username = principal.getName();
            userService.changePassword(username, request.getOldPassword(), request.getNewPassword());
            log.info("用户 '{}' 成功修改了密码。", username);
            return ResponseEntity.ok("密码修改成功。");
        } catch (IllegalArgumentException e) {
            log.warn("用户 '{}' 修改密码失败: {}", principal.getName(), e.getMessage());
            return ResponseEntity.badRequest().body(e.getMessage());
        } catch (Exception e) {
            log.error("修改用户 '{}' 密码时发生未知错误。", principal.getName(), e);
            return ResponseEntity.internalServerError().body("修改密码时发生内部错误。");
        }
    }
}
