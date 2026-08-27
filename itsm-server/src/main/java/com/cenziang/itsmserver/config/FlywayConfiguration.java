package com.cenziang.itsmserver.config;

import org.flywaydb.core.Flyway;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.sql.DataSource;

/**
 * Flyway 迁移配置。
 * <p>
 * 显式注册 Flyway 并在 Bean 初始化阶段执行 migrate，
 * 确保建表早于种子数据（CommandLineRunner）执行。
 * </p>
 */
@Configuration
public class FlywayConfiguration {

    @Bean(initMethod = "migrate")
    public Flyway flyway(DataSource dataSource) {
        return Flyway.configure()
                .dataSource(dataSource)
                .locations("classpath:db/migration")
                .baselineOnMigrate(true)
                .validateOnMigrate(true)
                .load();
    }
}
