package backend.daangnbasedbackend.global.config;

import backend.daangnbasedbackend.global.adapter.security.JwtAuthenticationFilter;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@RequiredArgsConstructor
public class SecurityConfig {
    private final JwtAuthenticationFilter jwtAuthenticationFilter;

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity httpSecurity) {
        httpSecurity.csrf(AbstractHttpConfigurer::disable) // REST API에서는 CSRF 보호 불필요 (세션을 쓰지 않으므로)
                .httpBasic(AbstractHttpConfigurer::disable) // 브라우저에서 뜨는 기본 인증 팝업을 막음
                .cors(Customizer.withDefaults())
                .formLogin(AbstractHttpConfigurer::disable) // 로그인 form 비활성화
                .sessionManagement(configure -> configure.sessionCreationPolicy(SessionCreationPolicy.STATELESS)) // JWT 기반 인증이므로 세션을 아예 생성하지 않음
                .authorizeHttpRequests(authorize ->
                                authorize
                                        .requestMatchers(HttpMethod.OPTIONS, "/v1/**", "/**").permitAll()
                                        .requestMatchers(
                                                "/actuator/health/readiness",
                                                "/actuator/health/liveness"
                                        ).permitAll()
                                        .requestMatchers(
                                                "/swagger-ui.html",
                                                "/swagger-ui/**",
                                                "/v3/api-docs/**"
                                        ).permitAll()
                                        .requestMatchers(
                                                "/v1/auth/kakao-social-login",
                                                "/v1/auth/refresh"
                                        ).permitAll()
                                        .anyRequest().authenticated()
                )
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);
        return httpSecurity.build();
    }

    /**
     * BCryptPasswordEncoder
     * - 비밀번호 해시화
     */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
