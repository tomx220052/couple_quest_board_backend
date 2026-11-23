package com.hannzer.cqb.auth;

import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

/**
 * 開發模式啟動時自動建立固定使用者；正式環境的使用者一律由開發者以 SQL 建立（規格書 7.5）。
 *
 * <p>非開發模式時不做任何事。
 */
@Component
public class DevUserInitializer implements ApplicationRunner {

    private static final String DEFAULT_DISPLAY_NAME = "開發者";

    private final AuthProperties properties;
    private final JdbcTemplate jdbc;

    DevUserInitializer(AuthProperties properties, JdbcTemplate jdbc) {
        this.properties = properties;
        this.jdbc = jdbc;
    }

    @Override
    public void run(ApplicationArguments args) {
        ensureDevUser();
    }

    /** 開發模式使用者不存在時建立；已存在則不變動 */
    public void ensureDevUser() {
        if (properties.mode() != AuthMode.DEV) {
            return;
        }
        String name = properties.devUserName();
        jdbc.update(
                "INSERT IGNORE INTO app_user (email, display_name) VALUES (?, ?)",
                properties.devUserEmail(),
                name == null || name.isBlank() ? DEFAULT_DISPLAY_NAME : name);
    }
}
