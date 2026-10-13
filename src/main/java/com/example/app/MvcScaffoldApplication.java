package com.example.app;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

/**
 * 增强 MVC 架构脚手架启动类。
 * <p>分层：Controller -&gt; Service -&gt; Manager -&gt; Mapper，遵循《Java 开发手册（黄山版）》应用分层规约。</p>
 */
@SpringBootApplication
@ConfigurationPropertiesScan
public class MvcScaffoldApplication {

    public static void main(String[] args) {
        SpringApplication.run(MvcScaffoldApplication.class, args);
    }
}
