package com.hannzer.cqb.areas;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.hannzer.cqb.support.IntegrationTest;
import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.ResultActions;

/** 領域 API（規格書 4.5、第 6 節） */
@DisplayName("領域 API")
class AreaApiTest extends IntegrationTest {

    @Nested
    @DisplayName("POST /api/areas 新增")
    class Create {

        @Test
        @DisplayName("回傳 201，依序排在最後，名稱去除前後空白")
        void appendsToEndAndTrims() throws Exception {
            createArea("健康")
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.name").value("健康"))
                    .andExpect(jsonPath("$.sortOrder").value(1))
                    .andExpect(jsonPath("$.hidden").value(false));

            createArea("  旅行  ")
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.id").isNumber())
                    .andExpect(jsonPath("$.name").value("旅行"))
                    .andExpect(jsonPath("$.sortOrder").value(2))
                    .andExpect(jsonPath("$.hidden").value(false));
        }

        @Test
        @DisplayName("記錄建立者")
        void recordsCreator() throws Exception {
            long id = idOf(createArea("健康"));

            Long createdBy =
                    jdbc.queryForObject(
                            "SELECT created_by FROM area WHERE id = ?", Long.class, id);
            Long devUserId =
                    jdbc.queryForObject(
                            "SELECT id FROM app_user WHERE email = ?", Long.class, DEV_EMAIL);
            assertThat(createdBy).isEqualTo(devUserId);
        }

        @Test
        @DisplayName("名稱重複回傳 409 AREA_NAME_DUPLICATED（含已隱藏的領域）")
        void duplicatedNameIncludingHidden() throws Exception {
            long health = idOf(createArea("健康"));
            mockMvc.perform(post("/api/areas/{id}/hide", health)).andExpect(status().isOk());

            createArea("健康")
                    .andExpect(status().isConflict())
                    .andExpect(jsonPath("$.code").value("AREA_NAME_DUPLICATED"))
                    .andExpect(jsonPath("$.message").value("領域名稱已存在"));
        }

        @Test
        @DisplayName("名稱空白、缺少或超過 50 字回傳 400；剛好 50 字可以")
        void nameValidation() throws Exception {
            createArea("   ")
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"))
                    .andExpect(jsonPath("$.message").value("領域名稱必填"));

            mockMvc.perform(post("/api/areas").contentType(MediaType.APPLICATION_JSON).content("{}"))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.message").value("領域名稱必填"));

            createArea("x".repeat(51))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.message").value("領域名稱不可超過 50 個字"));

            createArea("x".repeat(50)).andExpect(status().isCreated());
        }

        @Test
        @DisplayName("JSON 格式錯誤回傳 400 而非 500")
        void malformedJson() throws Exception {
            mockMvc.perform(
                            post("/api/areas")
                                    .contentType(MediaType.APPLICATION_JSON)
                                    .content("{ not json"))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"));
        }
    }

    @Nested
    @DisplayName("PATCH /api/areas/{id} 改名")
    class Rename {

        @Test
        @DisplayName("改名成功，名稱去除前後空白")
        void renames() throws Exception {
            long health = idOf(createArea("健康"));

            rename(health, "  運動  ")
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.id").value(health))
                    .andExpect(jsonPath("$.name").value("運動"))
                    .andExpect(jsonPath("$.sortOrder").value(1));
        }

        @Test
        @DisplayName("改成原本的名稱不算重複")
        void sameNameIsAllowed() throws Exception {
            long health = idOf(createArea("健康"));

            rename(health, "健康").andExpect(status().isOk());
        }

        @Test
        @DisplayName("改成其他領域已使用的名稱回傳 409")
        void duplicatedName() throws Exception {
            long health = idOf(createArea("健康"));
            createArea("旅行");

            rename(health, "旅行")
                    .andExpect(status().isConflict())
                    .andExpect(jsonPath("$.code").value("AREA_NAME_DUPLICATED"));
            listAreas().andExpect(jsonPath("$[*].name", contains("健康", "旅行")));
        }

        @Test
        @DisplayName("領域不存在回傳 404 AREA_NOT_FOUND")
        void notFound() throws Exception {
            rename(99999, "健康")
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.code").value("AREA_NOT_FOUND"))
                    .andExpect(jsonPath("$.message").value("找不到此領域"));
        }

        @Test
        @DisplayName("名稱空白回傳 400")
        void blankName() throws Exception {
            long health = idOf(createArea("健康"));

            rename(health, " ")
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.message").value("領域名稱必填"));
        }
    }

    @Nested
    @DisplayName("PUT /api/areas/order 調整排序")
    class Reorder {

        @Test
        @DisplayName("依傳入順序重寫排序（含已隱藏的領域），回傳全部領域")
        void reorders() throws Exception {
            long a = idOf(createArea("A"));
            long b = idOf(createArea("B"));
            long c = idOf(createArea("C"));
            mockMvc.perform(post("/api/areas/{id}/hide", b)).andExpect(status().isOk());

            reorder("[%d, %d, %d]".formatted(c, a, b))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$[*].name", contains("C", "A", "B")))
                    .andExpect(jsonPath("$[*].sortOrder", contains(1, 2, 3)))
                    .andExpect(jsonPath("$[*].hidden", contains(false, false, true)));

            listAreas().andExpect(jsonPath("$[*].name", contains("C", "A", "B")));
        }

        @Test
        @DisplayName("缺少領域、重複、含不存在的 id 回傳 400 INVALID_AREA_ORDER，排序不變")
        void rejectsIncompleteOrder() throws Exception {
            long a = idOf(createArea("A"));
            long b = idOf(createArea("B"));
            long c = idOf(createArea("C"));

            reorder("[%d, %d]".formatted(c, a))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.code").value("INVALID_AREA_ORDER"))
                    .andExpect(jsonPath("$.message").value("排序必須包含所有領域且不可重複，請重新整理後再試"));
            reorder("[%d, %d, %d]".formatted(c, a, a))
                    .andExpect(jsonPath("$.code").value("INVALID_AREA_ORDER"));
            reorder("[%d, %d, %d, 99999]".formatted(c, a, b))
                    .andExpect(jsonPath("$.code").value("INVALID_AREA_ORDER"));
            reorder("[%d, %d, 99999]".formatted(c, a))
                    .andExpect(jsonPath("$.code").value("INVALID_AREA_ORDER"));

            listAreas().andExpect(jsonPath("$[*].name", contains("A", "B", "C")));
        }

        @Test
        @DisplayName("ids 為空或缺少回傳 400 VALIDATION_FAILED")
        void rejectsEmptyIds() throws Exception {
            createArea("A");

            reorder("[]")
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"));
            mockMvc.perform(put("/api/areas/order").contentType(MediaType.APPLICATION_JSON).content("{}"))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"));
        }
    }

    @Nested
    @DisplayName("POST /api/areas/{id}/hide、/unhide 隱藏與恢復顯示")
    class HideAndUnhide {

        @Test
        @DisplayName("隱藏後仍出現在 GET /api/areas；恢復顯示回到原位")
        void hideAndUnhideKeepsPosition() throws Exception {
            createArea("健康");
            long travel = idOf(createArea("旅行"));
            createArea("財務");

            mockMvc.perform(post("/api/areas/{id}/hide", travel))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.id").value(travel))
                    .andExpect(jsonPath("$.hidden").value(true))
                    .andExpect(jsonPath("$.sortOrder").value(2));
            listAreas()
                    .andExpect(jsonPath("$[*].name", contains("健康", "旅行", "財務")))
                    .andExpect(jsonPath("$[*].hidden", contains(false, true, false)));

            mockMvc.perform(post("/api/areas/{id}/unhide", travel))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.hidden").value(false))
                    .andExpect(jsonPath("$.sortOrder").value(2));
        }

        @Test
        @DisplayName("重複隱藏或對未隱藏的領域恢復顯示，結果不變且不報錯")
        void idempotent() throws Exception {
            long health = idOf(createArea("健康"));

            mockMvc.perform(post("/api/areas/{id}/unhide", health))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.hidden").value(false));
            mockMvc.perform(post("/api/areas/{id}/hide", health)).andExpect(status().isOk());
            mockMvc.perform(post("/api/areas/{id}/hide", health))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.hidden").value(true));
        }

        @Test
        @DisplayName("領域不存在回傳 404 AREA_NOT_FOUND")
        void notFound() throws Exception {
            mockMvc.perform(post("/api/areas/99999/hide"))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.code").value("AREA_NOT_FOUND"));
            mockMvc.perform(post("/api/areas/99999/unhide"))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.code").value("AREA_NOT_FOUND"));
        }
    }

    @Nested
    @DisplayName("GET /api/areas 列表")
    class ListAll {

        @Test
        @DisplayName("沒有領域時回傳空陣列")
        void empty() throws Exception {
            listAreas().andExpect(status().isOk()).andExpect(jsonPath("$").isEmpty());
        }
    }

    @Nested
    @DisplayName("路徑參數")
    class PathId {

        @Test
        @DisplayName("id 不是正整數回傳 400 VALIDATION_FAILED")
        void invalidId() throws Exception {
            mockMvc.perform(
                            patch("/api/areas/abc")
                                    .contentType(MediaType.APPLICATION_JSON)
                                    .content("{\"name\":\"健康\"}"))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"));
            mockMvc.perform(post("/api/areas/0/hide"))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"));
            mockMvc.perform(post("/api/areas/-1/unhide"))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"));
        }
    }

    // ---------- helpers ----------

    private ResultActions createArea(String name) throws Exception {
        return mockMvc.perform(
                post("/api/areas").contentType(MediaType.APPLICATION_JSON).content(nameJson(name)));
    }

    private ResultActions rename(long id, String name) throws Exception {
        return mockMvc.perform(
                patch("/api/areas/{id}", id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(nameJson(name)));
    }

    private ResultActions reorder(String idsJsonArray) throws Exception {
        return mockMvc.perform(
                put("/api/areas/order")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"ids\": " + idsJsonArray + "}"));
    }

    private ResultActions listAreas() throws Exception {
        return mockMvc.perform(get("/api/areas"));
    }

    private static long idOf(ResultActions result) throws Exception {
        String body = result.andReturn().getResponse().getContentAsString();
        return ((Number) JsonPath.read(body, "$.id")).longValue();
    }

    /** 測試用名稱不含引號與反斜線，直接組 JSON */
    private static String nameJson(String name) {
        return "{\"name\": \"" + name + "\"}";
    }
}
