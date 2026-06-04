package backend.daangnbasedbackend.global.adapter.security;

import backend.daangnbasedbackend.global.application.TokenProviderPort;
import backend.daangnbasedbackend.auth.exception.AuthErrorType;
import backend.daangnbasedbackend.auth.exception.AuthException;
import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.security.Key;
import java.util.Date;

@Slf4j
@Component
@RequiredArgsConstructor
public class JwtTokenProvider implements TokenProviderPort {
    private final UserDetailsService userDetailsService;

    @Value("${jwt.secret}")
    private String SECRETKEY;

    @Value("${jwt.token.access-expiration-time}")
    private long accessTokenExpiration;

    @Value("${jwt.token.refresh-expiration-time}")
    private long refreshTokenExpiration;

    private Key key;

    @PostConstruct
    public void init() {
        byte[] keyBytes = SECRETKEY.getBytes(StandardCharsets.UTF_8);
        this.key = Keys.hmacShaKeyFor(keyBytes);
    }

    @Override
    public String createAccessToken(Long memberId) {
        return createToken(memberId, accessTokenExpiration);
    }

    @Override
    public String createRefreshToken(Long memberId) {
        return createToken(memberId, refreshTokenExpiration);
    }

    @Override
    public Long parseRefreshToken(String refreshToken) {
        try {
            Claims claims = parseClaims(refreshToken);
            return Long.valueOf(claims.getSubject());
        } catch (ExpiredJwtException e) {
            log.warn("Refresh token is expired: {}", e.getMessage());
            throw new AuthException(AuthErrorType.INVALID_REFRESH_TOKEN);
        } catch (JwtException | IllegalArgumentException e) {
            log.error("Failed to parse refresh token: {}", e.getMessage());
            throw new AuthException(AuthErrorType.INVALID_REFRESH_TOKEN);
        }
    }

    @Override
    public Long getRefreshTokenExpiration() {
        return refreshTokenExpiration;
    }

    @Override
    public Long getAccessTokenExpiration() {
        return accessTokenExpiration;
    }

    @Override
    public Authentication getAuthentication(String token) {
        try {
            Claims claims = parseClaims(token);
            long memberId = Long.parseLong(claims.getSubject());
            UserDetails userDetails = userDetailsService.loadUserByUsername(Long.toString(memberId));
            return new UsernamePasswordAuthenticationToken(userDetails, "", userDetails.getAuthorities());

        } catch (ExpiredJwtException e) {
            log.warn("Access token expired: {}", e.getMessage());
            throw new AuthException(AuthErrorType.TOKEN_EXPIRED);

        } catch (JwtException | IllegalArgumentException e) {
            log.warn("Invalid access token: {}", e.getMessage());
            throw new AuthException(AuthErrorType.INVALID_ACCESS_TOKEN);
        }
    }

    private String createToken(Long memberId, long expiration) {
        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + expiration);

        return Jwts.builder()
                .setSubject(memberId.toString())
                .setIssuedAt(now)
                .setExpiration(expiryDate)
                .signWith(key, SignatureAlgorithm.HS512)
                .compact();
    }

    private Claims parseClaims(String token) {
        return Jwts.parserBuilder()
                .setSigningKey(key)
                .build()
                .parseClaimsJws(token)
                .getBody();
    }
}
