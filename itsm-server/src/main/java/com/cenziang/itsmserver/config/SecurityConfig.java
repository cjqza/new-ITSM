package com.cenziang.itsmserver.config;

import com.cenziang.itsmserver.service.AuthTokenService;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

/**
 * Spring Security 安全配置。
 * <p>
 * 这里关闭无状态会话与表单登录，注册一个 JWT 解析过滤器，将令牌身份写入请求属性。
 * </p>
 */
@Configuration
public class SecurityConfig {

    /**
     * 构建安全过滤链。
     */
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http, AuthTokenService tokenService) throws Exception {
        http.csrf(AbstractHttpConfigurer::disable)
                .headers(headers -> headers.frameOptions(frame -> frame.sameOrigin()))
                .formLogin(AbstractHttpConfigurer::disable)
                .httpBasic(AbstractHttpConfigurer::disable)
                .sessionManagement(AbstractHttpConfigurer::disable)
                .authorizeHttpRequests(auth -> auth.anyRequest().permitAll())
                .addFilterBefore(jwtAuthenticationFilter(tokenService), UsernamePasswordAuthenticationFilter.class);
        return http.build();
    }

    /**
     * 注册 JWT 身份解析过滤器。
     */
    @Bean
    public OncePerRequestFilter jwtAuthenticationFilter(AuthTokenService tokenService) {
        return new OncePerRequestFilter() {
            @Override
            protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
                    throws ServletException, IOException {
                String authorization = request.getHeader("Authorization");
                if (authorization != null && authorization.startsWith("Bearer ")) {
                    try {
                        AuthTokenService.TokenClaims claims = tokenService.parseAndValidate(authorization.substring(7).trim());
                        if ("access".equalsIgnoreCase(claims.tokenType())) {
                            request.setAttribute("auth.userId", claims.userId());
                            request.setAttribute("auth.tenantId", claims.tenantId());
                            request.setAttribute("auth.roles", claims.roles());
                            request.setAttribute("auth.permissionsVersion", claims.permissionsVersion());
                            request.setAttribute("auth.authVersion", claims.authVersion());
                        }
                    } catch (JwtException | IllegalArgumentException ignored) {
                        // 认证失败交给业务异常处理，避免过滤器抛出堆栈。
                    }
                }
                chain.doFilter(request, response);
            }
        };
    }
}