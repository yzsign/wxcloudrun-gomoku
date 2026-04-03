package com.gomoku.sync.config;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.annotation.EnableTransactionManagement;
import org.springframework.web.client.RestTemplate;

@Configuration
@MapperScan("com.gomoku.sync.mapper")
@EnableTransactionManagement
public class MybatisConfig {

    @Bean
    public RestTemplate restTemplate() {
        return new RestTemplate();
    }
}
