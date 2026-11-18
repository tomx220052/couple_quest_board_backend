package com.hannzer.cqb.auth;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** 依 AUTH_MODE 決定如何取得登入者 email */
@Configuration
class AuthConfig {

    private static final Logger log = LoggerFactory.getLogger(AuthConfig.class);

    @Bean
    EmailResolver emailResolver(AuthProperties properties) {
        return switch (properties.mode()) {
            case DEV -> {
                String email = properties.devUserEmail();
                if (email == null || email.isBlank()) {
                    throw new IllegalStateException("AUTH_MODE=dev 時必須設定 DEV_USER_EMAIL");
                }
                log.warn("AUTH_MODE=dev：所有請求皆視為 {}，正式環境不可使用", email);
                yield new DevEmailResolver(email);
            }
            case ACCESS -> new AccessEmailResolver();
        };
    }
}
