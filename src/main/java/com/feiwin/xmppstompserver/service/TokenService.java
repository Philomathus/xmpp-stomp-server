package com.feiwin.xmppstompserver.service;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.Key;
import java.util.Map;

@Service
public class TokenService {

    private static Key KEY_SECRET;

    @Value( "${jjwt.secret}" )
    private void setKeySecret( String secret ) {
        TokenService.KEY_SECRET = Keys.hmacShaKeyFor( secret.getBytes( StandardCharsets.UTF_8 ) );
    }

    /**
     * 从数据声明生成令牌
     *
     * @param claims 数据声明
     *
     * @return 令牌
     */
    public String createToken( Map<String, String> claims ) {
        return Jwts.builder().setClaims( claims ).signWith( KEY_SECRET ).compact();
    }

    /**
     * 从令牌中获取数据声明
     *
     * @param token 令牌
     *
     * @return 数据声明
     */
    public Claims parseToken(String token ) {
        return Jwts.parserBuilder().setSigningKey( KEY_SECRET ).build().parseClaimsJws( token ).getBody();
    }
}

