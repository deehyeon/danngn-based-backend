package backend.daangnbasedbackend.global.adapter.security;

import backend.daangnbasedbackend.global.application.provided.TokenProviderPort;
import backend.daangnbasedbackend.auth.exception.AuthException;
import backend.daangnbasedbackend.global.exception.ErrorType;
import backend.daangnbasedbackend.global.webapi.response.ApiResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {
    private final TokenProviderPort tokenProviderPort;
    private final ObjectMapper objectMapper;

    // JWT 검증을 제외할 경로
    private static final List<String> EXCLUDE_URLS = List.of(
            "/swagger-ui.html",
            "/swagger-ui/",
            "/v1/api-docs",
            "/actuator/health/readiness",
            "/actuator/health/liveness",
            "/v1/auth/kakao-social-login",   // 소셜 로그인 (AT 없음)
            "/v1/auth/refresh"  // AT가 만료된 상태로 호출되므로 필터 우회 필수
    );

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getRequestURI();
        return EXCLUDE_URLS.stream().anyMatch(path::startsWith);
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        String token = extractToken(request);

        if (token != null) {
            try {
                Authentication auth = tokenProviderPort.getAuthentication(token);
                SecurityContextHolder.getContext().setAuthentication(auth);
                log.debug("SecurityContext 인증 설정 — user: {}", auth.getName());
            } catch (AuthException e) {
                // @RestControllerAdvice는 필터 예외를 처리하지 못하므로 직접 응답 작성
                sendErrorResponse(response, e.getErrorType());
                return;
            }
        }

        filterChain.doFilter(request, response);
    }

    private void sendErrorResponse(HttpServletResponse response, ErrorType errorType) throws IOException {
        response.setStatus(errorType.getStatus().value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");
        response.getWriter().write(objectMapper.writeValueAsString(ApiResponse.error(errorType)));
    }

    private String extractToken(HttpServletRequest request) {
        String bearerToken = request.getHeader("Authorization");
        if (bearerToken != null && bearerToken.startsWith("Bearer ")) {
            return bearerToken.substring(7);
        }
        return null;
    }
}
