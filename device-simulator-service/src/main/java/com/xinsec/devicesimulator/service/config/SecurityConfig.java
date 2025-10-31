package com.xinsec.devicesimulator.service.config;

import com.xinsec.devicesimulator.service.auth.JwtRequestFilter;
import com.xinsec.devicesimulator.service.auth.UserDetailsServiceImpl;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig extends WebSecurityConfigurerAdapter {

    private final UserDetailsServiceImpl userDetailsService;
    private final JwtRequestFilter jwtRequestFilter; // 注入JWT过滤器

    @Override
    protected void configure(AuthenticationManagerBuilder auth) throws Exception {
        auth.userDetailsService(userDetailsService).passwordEncoder(passwordEncoder());
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    @Override
    public AuthenticationManager authenticationManagerBean() throws Exception {
        return super.authenticationManagerBean();
    }

    @Override
    protected void configure(HttpSecurity http) throws Exception {
        http
            // 禁用 CSRF，因为我们使用无状态的 JWT
            .csrf().disable()
            // 配置URL授权规则
            .authorizeRequests()
                // 允许匿名访问静态资源和登录页面
                .antMatchers("/", "/index.html", "/css/**", "/js/**", "/favicon.ico").permitAll()
                // 允许匿名访问登录接口
                .antMatchers("/api/auth/login").permitAll()
                // 其他所有 /api/** 的请求都需要认证
                .antMatchers("/api/**").authenticated()
                // 其他任何请求都允许访问（例如 WebSocket 的 /ws 端点）
                .anyRequest().permitAll()
            .and()
            // 配置会话管理为无状态（STATELESS），不使用 Session
            .sessionManagement()
                .sessionCreationPolicy(SessionCreationPolicy.STATELESS);

        // 将JWT过滤器添加到Spring Security的过滤器链中，确保它在用户名密码认证过滤器之前执行
        http.addFilterBefore(jwtRequestFilter, UsernamePasswordAuthenticationFilter.class);
    }
}
