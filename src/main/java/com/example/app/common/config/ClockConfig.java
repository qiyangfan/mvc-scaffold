package com.example.app.common.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;

/**
 * 业务时钟装配：业务代码一律经注入 Clock 取当前时刻，
 * 生产取系统时钟，测试可替换为固定时钟，时间语义确定性可测。
 */
@Configuration
public class ClockConfig {

    @Bean
    public Clock clock() {
        return Clock.systemDefaultZone();
    }
}
