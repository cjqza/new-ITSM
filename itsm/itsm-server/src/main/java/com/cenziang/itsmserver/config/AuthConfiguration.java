package com.cenziang.itsmserver.config;

import com.cenziang.itsmserver.config.properties.ItsmAuthProperties;
import com.cenziang.itsmserver.config.properties.ItsmRegisterProperties;
import com.cenziang.itsmserver.config.properties.ItsmSecurityProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * 认证与安全配置绑定。
 */
@Configuration
@EnableConfigurationProperties({ItsmAuthProperties.class, ItsmSecurityProperties.class, ItsmRegisterProperties.class})
public class AuthConfiguration {
}