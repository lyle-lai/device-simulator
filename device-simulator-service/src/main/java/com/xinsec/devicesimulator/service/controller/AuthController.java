package com.xinsec.devicesimulator.service.controller;

import com.xinsec.devicesimulator.service.auth.JwtUtil;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 负责用户认证的控制器，主要是登录接口。
 */
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthenticationManager authenticationManager;
    private final UserDetailsService userDetailsService;
    private final JwtUtil jwtUtil;

    /**
     * 用户登录接口。
     * @param request 包含用户名和密码的认证请求。
     * @return 如果认证成功，返回包含JWT的响应；否则返回401未授权。
     * @throws Exception 认证过程中可能抛出异常。
     */
    @PostMapping("/login")
    public ResponseEntity<?> createAuthenticationToken(@RequestBody AuthenticationRequest request) throws Exception {
        try {
            // 使用 AuthenticationManager 进行用户认证
            authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.getUsername(), request.getPassword())
            );
        } catch (BadCredentialsException e) {
            // 捕获认证失败的异常，返回401
            return ResponseEntity.status(401).body("用户名或密码错误");
        }

        // 如果认证成功，加载用户信息并生成JWT
        final UserDetails userDetails = userDetailsService.loadUserByUsername(request.getUsername());
        final String jwt = jwtUtil.generateToken(userDetails);

        // 返回包含JWT的成功响应
        return ResponseEntity.ok(new AuthenticationResponse(jwt));
    }

    /**
     * 认证请求的数据传输对象 (DTO)。
     */
    @Data
    @AllArgsConstructor
    public static class AuthenticationRequest {
        private String username;
        private String password;
    }

    /**
     * 认证响应的数据传输对象 (DTO)。
     */
    @Data
    @AllArgsConstructor
    public static class AuthenticationResponse {
        private String jwt;
    }
}
