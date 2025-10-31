package com.xinsec.devicesimulator.service.auth;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.xinsec.devicesimulator.service.entity.UserEntity;
import com.xinsec.devicesimulator.service.mapper.UserMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Spring Security 用于加载用户信息的服务实现。
 */
@Service
@RequiredArgsConstructor
public class UserDetailsServiceImpl implements UserDetailsService {

    private final UserMapper userMapper;

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        // 从数据库中根据用户名查询用户
        QueryWrapper<UserEntity> queryWrapper = new QueryWrapper<>();
        queryWrapper.lambda().eq(UserEntity::getUsername, username);
        UserEntity userEntity = userMapper.selectOne(queryWrapper);

        if (userEntity == null) {
            throw new UsernameNotFoundException("用户 '" + username + "' 不存在。");
        }

        // 将逗号分隔的角色字符串转换为 GrantedAuthority 列表
        List<SimpleGrantedAuthority> authorities = Arrays.stream(userEntity.getRoles().split(","))
                .map(SimpleGrantedAuthority::new)
                .collect(Collectors.toList());

        // 返回 Spring Security 的 User 对象
        return new User(userEntity.getUsername(), userEntity.getPassword(), userEntity.isEnabled(), 
                        true, true, true, authorities);
    }
}
