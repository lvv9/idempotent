package me.liuweiqiang.idempotent;

import com.alibaba.druid.pool.DruidDataSource;
import com.alibaba.druid.spring.boot.autoconfigure.DruidDataSourceBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class IdempotentConfig {

    @Bean
    public DruidDataSource druidDataSource() {
        return DruidDataSourceBuilder.create().build();
    }
}
