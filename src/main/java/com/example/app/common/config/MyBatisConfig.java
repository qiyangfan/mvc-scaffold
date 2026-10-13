package com.example.app.common.config;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.context.annotation.Configuration;

/**
 * MyBatis 配置：统一扫描 Mapper 接口。
 * <p>下划线转驼峰、mapper XML 位置等在 application.yml 中配置。</p>
 */
@Configuration
@MapperScan("com.example.app.mapper")
public class MyBatisConfig {
}
