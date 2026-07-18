package com.dev.videoStreaming.video.infrastructure;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.beans.factory.annotation.Value;
import org.redisson.Redisson;
import org.redisson.api.RedissonClient;
import org.redisson.config.Config;

@Configuration
public class redisConfig {

    @Value("${spring.redis.host:localhost}")
    private String redisHost;

    @Value("${spring.redis.port:6379}")
    private int redisPort;

    @Value("${spring.redis.password:}")
    private String redisPassword;
    
    @Bean
    public RedissonClient redissonClient() {
        Config config = new Config();
        config.setPassword(redisPassword);
        config.useSingleServer()
              .setAddress("redis://" + redisHost + ":" + redisPort);
        return Redisson.create(config);
    }
}