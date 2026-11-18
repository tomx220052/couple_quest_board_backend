package com.hannzer.cqb.support;

import com.hannzer.cqb.auth.DevUserInitializer;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.mysql.MySQLContainer;

/**
 * 整合測試基底：真的 MySQL 8.4（Testcontainers）＋完整 Spring context＋MockMvc。
 *
 * <p>預設以開發模式登入（固定使用者）。沒有 Docker 時，可設定環境變數 TEST_DB_URL、TEST_DB_USER、TEST_DB_PASSWORD
 * 指向既有的 MySQL（測試會清空該資料庫的資料，請使用專用的測試資料庫）。
 */
@SpringBootTest
@AutoConfigureMockMvc
@TestPropertySource(
        properties = {
            "cqb.auth.mode=dev",
            "cqb.auth.dev-user-email=" + IntegrationTest.DEV_EMAIL,
            "cqb.auth.dev-user-name=" + IntegrationTest.DEV_NAME,
        })
public abstract class IntegrationTest {

    public static final String DEV_EMAIL = "dev@example.com";
    public static final String DEV_NAME = "開發者";

    private static final String EXTERNAL_DB_URL = blankToNull(System.getenv("TEST_DB_URL"));

    /** 所有測試類別共用一個容器，JVM 結束時由 Testcontainers 回收 */
    private static final MySQLContainer MYSQL = EXTERNAL_DB_URL == null ? startMysql() : null;

    @Autowired protected MockMvc mockMvc;

    @Autowired protected JdbcTemplate jdbc;

    @Autowired private DevUserInitializer devUserInitializer;

    @DynamicPropertySource
    static void datasource(DynamicPropertyRegistry registry) {
        if (EXTERNAL_DB_URL != null) {
            registry.add("spring.datasource.url", () -> EXTERNAL_DB_URL);
            registry.add("spring.datasource.username", () -> envOrDefault("TEST_DB_USER", "root"));
            registry.add("spring.datasource.password", () -> envOrDefault("TEST_DB_PASSWORD", ""));
        } else {
            registry.add("spring.datasource.url", MYSQL::getJdbcUrl);
            registry.add("spring.datasource.username", MYSQL::getUsername);
            registry.add("spring.datasource.password", MYSQL::getPassword);
        }
    }

    /** 每個測試前清空資料，並重新建立開發模式使用者，測試彼此獨立 */
    @BeforeEach
    void resetDatabase() {
        jdbc.update("DELETE FROM subtask");
        jdbc.update("DELETE FROM goal");
        jdbc.update("DELETE FROM domain");
        jdbc.update("DELETE FROM app_user");
        devUserInitializer.ensureDevUser();
    }

    private static MySQLContainer startMysql() {
        MySQLContainer container =
                new MySQLContainer("mysql:8.4")
                        .withUrlParam("connectionTimeZone", "+08:00")
                        .withUrlParam("forceConnectionTimeZoneToSession", "true");
        container.start();
        return container;
    }

    private static String envOrDefault(String name, String defaultValue) {
        String value = blankToNull(System.getenv(name));
        return value == null ? defaultValue : value;
    }

    private static String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value;
    }
}
