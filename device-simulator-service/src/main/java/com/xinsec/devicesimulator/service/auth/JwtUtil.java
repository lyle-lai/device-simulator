package com.xinsec.devicesimulator.service.auth;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import java.security.Key;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

/**
 * 用于处理JWT（JSON Web Token）的工具类。
 * 负责生成、解析和验证令牌。
 */
@Service
public class JwtUtil {

    // 注意：这是一个硬编码的密钥，在生产环境中应通过配置文件注入，并且更加复杂！
    private final String SECRET_KEY = "device-simulator-secret-key-for-jwt-2024-and-it-must-be-long-enough";
    private Key key;

    @PostConstruct
    public void init() {
        // 从密钥字符串生成一个安全的Key对象
        this.key = Keys.hmacShaKeyFor(SECRET_KEY.getBytes());
    }

    /**
     * 从令牌中提取用户名。
     * @param token JWT令牌
     * @return 用户名
     */
    public String extractUsername(String token) {
        return extractClaim(token, Claims::getSubject);
    }

    /**
     * 从令牌中提取过期时间。
     * @param token JWT令牌
     * @return 过期时间
     */
    public Date extractExpiration(String token) {
        return extractClaim(token, Claims::getExpiration);
    }

    /**
     * 从令牌中提取指定的声明。
     * @param token JWT令牌
     * @param claimsResolver 用于提取声明的函数
     * @return 声明的值
     */
    public <T> T extractClaim(String token, Function<Claims, T> claimsResolver) {
        final Claims claims = extractAllClaims(token);
        return claimsResolver.apply(claims);
    }

    private Claims extractAllClaims(String token) {
        // 使用新的 parserBuilder API
        return Jwts.parserBuilder().setSigningKey(key).build().parseClaimsJws(token).getBody();
    }

    /**
     * 检查令牌是否已过期。
     * @param token JWT令牌
     * @return 如果已过期则返回true
     */
    private Boolean isTokenExpired(String token) {
        return extractExpiration(token).before(new Date());
    }

    /**
     * 根据用户信息生成一个新的JWT令牌。
     * @param userDetails 用户信息
     * @return JWT令牌字符串
     */
    public String generateToken(UserDetails userDetails) {
        Map<String, Object> claims = new HashMap<>();
        return createToken(claims, userDetails.getUsername());
    }

    private String createToken(Map<String, Object> claims, String subject) {
        return Jwts.builder().setClaims(claims).setSubject(subject).setIssuedAt(new Date(System.currentTimeMillis()))
                .setExpiration(new Date(System.currentTimeMillis() + 1000 * 60 * 60 * 10)) // 10小时有效期
                .signWith(key, SignatureAlgorithm.HS256).compact(); // 使用新的 signWith 方法
    }

    /**
     * 验证令牌是否有效。
     * @param token JWT令牌
     * @param userDetails 用户信息
     * @return 如果令牌对该用户有效，则返回true
     */
    public Boolean validateToken(String token, UserDetails userDetails) {
        final String username = extractUsername(token);
        return (username.equals(userDetails.getUsername()) && !isTokenExpired(token));
    }
}
