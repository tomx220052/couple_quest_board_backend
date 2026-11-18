package com.hannzer.cqb.common;

import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.hannzer.cqb.support.IntegrationTest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

/** 統一錯誤格式 { code, message }（規格書第 6 節）。以測試專用的端點觸發各類錯誤。 */
@DisplayName("統一錯誤格式")
@Import(ErrorHandlingTest.ProbeController.class)
class ErrorHandlingTest extends IntegrationTest {

    @Test
    @DisplayName("不存在的 API 路徑回傳 404 NOT_FOUND")
    void unknownPath() throws Exception {
        mockMvc.perform(get("/api/no-such-api"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("NOT_FOUND"))
                .andExpect(jsonPath("$.message").value("找不到此 API"));
    }

    @Test
    @DisplayName("路徑存在但方法不支援時回傳 404 NOT_FOUND")
    void unsupportedMethod() throws Exception {
        mockMvc.perform(delete("/api/health"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("NOT_FOUND"));
    }

    @Test
    @DisplayName("JSON 格式錯誤回傳 400 VALIDATION_FAILED 而非 500")
    void malformedJson() throws Exception {
        mockMvc.perform(
                        post("/api/test/probe")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{\"name\": "))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"))
                .andExpect(jsonPath("$.message").value("請求格式錯誤"));
    }

    @Test
    @DisplayName("欄位驗證失敗回傳 400，多則訊息以「；」串接")
    void validationMessagesJoined() throws Exception {
        mockMvc.perform(
                        post("/api/test/probe")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{\"name\": \"  \"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"))
                .andExpect(
                        jsonPath("$.message")
                                .value(
                                        allOf(
                                                containsString("名稱必填"),
                                                containsString("數量必填"),
                                                containsString("；"))));
    }

    @Test
    @DisplayName("未預期的錯誤回傳 500 INTERNAL_ERROR，不外洩內部訊息")
    void unexpectedError() throws Exception {
        mockMvc.perform(get("/api/test/boom"))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.code").value("INTERNAL_ERROR"))
                .andExpect(jsonPath("$.message").value("伺服器發生錯誤"))
                .andExpect(jsonPath("$.message").value(not(containsString("內部細節"))));
    }

    /** 只在此測試載入的端點 */
    @RestController
    static class ProbeController {

        record ProbeRequest(
                @NotBlank(message = "名稱必填") String name,
                @NotNull(message = "數量必填") Integer count) {}

        @PostMapping("/api/test/probe")
        ProbeRequest probe(@Valid @RequestBody ProbeRequest body) {
            return body;
        }

        @GetMapping("/api/test/boom")
        void boom() {
            throw new IllegalStateException("內部細節");
        }
    }
}
