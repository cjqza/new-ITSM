package com.cenziang.itsmserver;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * ITSM 后端启动入口。
 * <p>
 * 这个应用承载一期全部 REST 接口、认证、缓存、消息与持久化能力。
 * </p>
 */
@EnableScheduling
@ConfigurationPropertiesScan
@MapperScan("com.cenziang.itsmserver.infrastructure.persistence.mapper")
@SpringBootApplication
public class ItsmServerApplication {

    /**
     * 应用启动方法。
     *
     * @param args 启动参数
     */
    public static void main(String[] args) {
        SpringApplication.run(ItsmServerApplication.class, args);
    }

}
