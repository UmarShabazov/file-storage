package com.example.file_storage.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.session.data.redis.config.annotation.web.http.EnableRedisIndexedHttpSession;

@Configuration
@Profile("!test")
@EnableRedisIndexedHttpSession
public class SessionConfig {
}
