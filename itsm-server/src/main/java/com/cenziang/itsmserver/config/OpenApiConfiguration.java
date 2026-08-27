package com.cenziang.itsmserver.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import org.springdoc.core.models.GroupedOpenApi;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Knife4j / springdoc OpenAPI 配置。
 * <p>
 * 这个配置为 ITSM 一期后端生成接口文档，并允许按业务域分组查看。
 * </p>
 */
@Configuration
public class OpenApiConfiguration {

    /**
     * 构建全局 OpenAPI 元信息。
     *
     * @return OpenAPI 文档元信息
     */
    @Bean
    public OpenAPI itsmOpenApi() {
        return new OpenAPI()
                .info(new Info()
                        .title("ITSM 桌面工单处理系统 API")
                        .description("一期核心接口：认证、会话、Agent 编排、工单、客服、字典、权限、评价。")
                        .version("v1.0")
                        .contact(new Contact().name("ITSM Team"))
                        .license(new License().name("Internal Use Only")));
    }

    /**
     * 构建业务接口分组。
     *
     * @return 业务接口分组
     */
    @Bean
    public GroupedOpenApi publicApi() {
        return GroupedOpenApi.builder()
                .group("itsm-api")
                .pathsToMatch("/api/**")
                .build();
    }
}