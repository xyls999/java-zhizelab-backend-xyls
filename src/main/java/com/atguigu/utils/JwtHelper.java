package com.atguigu.utils;

import com.baomidou.mybatisplus.core.toolkit.StringUtils;
import io.jsonwebtoken.*;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.Date;

@Component
@ConfigurationProperties(prefix = "jwt.token")
public class JwtHelper {

    private long tokenExpiration; // 单位：分钟（来自 yml）
    private String tokenSignKey;  // 签名秘钥

    // 生成token
    public String createToken(Long userId) {
        return Jwts.builder()
                .setSubject("NEWS-USER")
                .setExpiration(new Date(System.currentTimeMillis() + tokenExpiration * 1000L * 60L))
                .claim("userId", userId)
                .signWith(SignatureAlgorithm.HS512, tokenSignKey)
                .compressWith(CompressionCodecs.GZIP)
                .compact();
    }

    // 从token获取userId
    public Long getUserId(String token) {
        if (StringUtils.isBlank(token)) return null;
        Claims claims = Jwts.parser()
                .setSigningKey(tokenSignKey)
                .parseClaimsJws(token)
                .getBody();

        Object uidObj = claims.get("userId");
        if (uidObj == null) return null;
        if (uidObj instanceof Number) return ((Number) uidObj).longValue();
        return Long.valueOf(uidObj.toString());
    }

    // 判断token是否过期：true=过期/无效，false=未过期
    public boolean isExpiration(String token) {
        try {
            Date exp = Jwts.parser()
                    .setSigningKey(tokenSignKey)
                    .parseClaimsJws(token)
                    .getBody()
                    .getExpiration();
            return exp.before(new Date());
        } catch (Exception e) {
            return true; // 解析失败也当成无效/过期
        }
    }

    public long getTokenExpiration() {
        return tokenExpiration;
    }

    public void setTokenExpiration(long tokenExpiration) {
        this.tokenExpiration = tokenExpiration;
    }

    public String getTokenSignKey() {
        return tokenSignKey;
    }

    public void setTokenSignKey(String tokenSignKey) {
        this.tokenSignKey = tokenSignKey;
    }
}
