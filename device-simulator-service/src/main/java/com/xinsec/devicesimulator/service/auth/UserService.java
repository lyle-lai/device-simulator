package com.xinsec.devicesimulator.service.auth;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.xinsec.devicesimulator.service.entity.UserEntity;
import com.xinsec.devicesimulator.service.mapper.UserMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

/**
 * 用户服务，处理用户相关的业务逻辑。
 */
@Service
@RequiredArgsConstructor
public class UserService {

    private final UserMapper userMapper;
    private final PasswordEncoder passwordEncoder;

    /**
     * 修改用户密码。
     * @param username 要修改密码的用户名。
     * @param oldPassword 用户的当前密码。
     * @param newPassword 用户的新密码。
     * @throws IllegalArgumentException 如果旧密码不正确。
     */
    public void changePassword(String username, String oldPassword, String newPassword) {
        QueryWrapper<UserEntity> queryWrapper = new QueryWrapper<>();
        queryWrapper.lambda().eq(UserEntity::getUsername, username);
        UserEntity user = userMapper.selectOne(queryWrapper);

        if (user == null) {
            // 理论上，在已登录的情况下不应该发生
            throw new IllegalStateException("找不到当前登录的用户。");
        }

        // 验证旧密码是否正确
        if (!passwordEncoder.matches(oldPassword, user.getPassword())) {
            throw new IllegalArgumentException("当前密码不正确。");
        }

        // 将新密码加密并更新到数据库
        user.setPassword(passwordEncoder.encode(newPassword));
        userMapper.updateById(user);
    }
}
