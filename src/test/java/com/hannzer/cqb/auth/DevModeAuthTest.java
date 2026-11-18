package com.hannzer.cqb.auth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.hannzer.cqb.support.IntegrationTest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

@DisplayName("開發模式登入（AUTH_MODE=dev）")
class DevModeAuthTest extends IntegrationTest {

    @Autowired private DevUserInitializer initializer;

    @Test
    @DisplayName("GET /api/auth/me 回傳固定的開發模式使用者")
    void meReturnsDevUser() throws Exception {
        Long id =
                jdbc.queryForObject(
                        "SELECT id FROM app_user WHERE email = ?", Long.class, DEV_EMAIL);

        mockMvc.perform(get("/api/auth/me"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(id))
                .andExpect(jsonPath("$.email").value(DEV_EMAIL))
                .andExpect(jsonPath("$.displayName").value(DEV_NAME));
    }

    @Test
    @DisplayName("自動在 app_user 建立開發模式使用者，重複執行不會重複建立")
    void createsDevUserIdempotently() {
        initializer.ensureDevUser();

        var names =
                jdbc.queryForList(
                        "SELECT display_name FROM app_user WHERE email = ?",
                        String.class,
                        DEV_EMAIL);
        assertThat(names).containsExactly(DEV_NAME);
    }

    @Test
    @DisplayName("email 不在 app_user 時回傳 403 USER_NOT_REGISTERED")
    void unregisteredUserIsForbidden() throws Exception {
        jdbc.update("DELETE FROM app_user");

        mockMvc.perform(get("/api/auth/me"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("USER_NOT_REGISTERED"))
                .andExpect(jsonPath("$.message").value("此帳號沒有使用權限"));
    }

    @Test
    @DisplayName("GET /api/health 回傳 ok")
    void health() throws Exception {
        mockMvc.perform(get("/api/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("ok"));
    }
}
