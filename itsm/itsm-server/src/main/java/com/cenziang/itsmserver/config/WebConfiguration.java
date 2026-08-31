package com.cenziang.itsmserver.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Web MVC 配置。
 * <p>
 * 一期前端为独立静态工程，联调阶段放开 CORS，便于本地跨域调试。
 * 上线时应收紧为白名单域名。
 * </p>
 */
@Configuration
public class WebConfiguration implements WebMvcConfigurer {

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/**")
                .allowedOriginPatterns("*")
                .allowedMethods("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS")
                .allowedHeaders("*")
                .exposedHeaders("Authorization", "X-Tenant-Id", "X-Trace-Id")
                .allowCredentials(false)
                .maxAge(3600);
    }
}