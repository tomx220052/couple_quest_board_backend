package com.hannzer.cqb.auth;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.hannzer.cqb.support.IntegrationTest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.context.TestPropertySource;

/** 正式登入模式。JWT 驗證在 B4 實作，B0 先確認沒有 JWT 的請求會被拒絕。 */
@DisplayName("正式登入模式（AUTH_MODE=access）")
@TestPropertySource(properties = "cqb.auth.mode=access")
class AccessModeAuthTest extends IntegrationTest {

    @Test
    @DisplayName("沒有 JWT 時 API 回傳 401 UNAUTHENTICATED")
    void requestWithoutJwtIsUnauthenticated() throws Exception {
        mockMvc.perform(get("/api/auth/me"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("UNAUTHENTICATED"))
                .andExpect(jsonPath("$.message").value("尚未登入"));
    }

    @Test
    @DisplayName("GET /api/health 不需登入")
    void healthDoesNotRequireLogin() throws Exception {
        mockMvc.perform(get("/api/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("ok"));
    }
}
