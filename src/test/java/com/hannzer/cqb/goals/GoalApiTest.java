package com.hannzer.cqb.goals;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.hannzer.cqb.support.ApiFixtures;
import com.hannzer.cqb.support.IntegrationTest;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/** 目標 API（規格書 4.2、4.4、第 6 節） */
@DisplayName("目標 API")
class GoalApiTest extends IntegrationTest {

    private ApiFixtures api;
    private long areaId;

    @BeforeEach
    void setUpFixtures() throws Exception {
        api = new ApiFixtures(mockMvc);
        areaId = api.area("健康");
    }

    @Nested
    @DisplayName("POST /api/goals 新增")
    class Create {

        @Test
        @DisplayName("回傳 201 與目標內容：ACTIVE、尚無子任務、建立者")
        void createsGoal() throws Exception {
            Long devUserId =
                    jdbc.queryForObject(
                            "SELECT id FROM app_user WHERE email = ?", Long.class, DEV_EMAIL);

            api.createGoal(
                            "{\"areaId\": %d, \"title\": \"  半年內體脂降到 18%%  \", \"dueDate\": \"2027-04-30\"}"
                                    .formatted(areaId))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.id").isNumber())
                    .andExpect(jsonPath("$.areaId").value(areaId))
                    .andExpect(jsonPath("$.title").value("半年內體脂降到 18%"))
                    .andExpect(jsonPath("$.dueDate").value("2027-04-30"))
                    .andExpect(jsonPath("$.status").value("ACTIVE"))
                    .andExpect(jsonPath("$.progress").doesNotExist())
                    .andExpect(jsonPath("$.createdBy.id").value(devUserId))
                    .andExpect(jsonPath("$.createdBy.displayName").value(DEV_NAME))
                    .andExpect(jsonPath("$.subtasks").isEmpty());
        }

        @Test
        @DisplayName("截止日可省略或為 null，回傳 null")
        void dueDateOptional() throws Exception {
            api.createGoal("{\"areaId\": %d, \"title\": \"A\"}".formatted(areaId))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.dueDate").value((Object) null));
            api.createGoal("{\"areaId\": %d, \"title\": \"B\", \"dueDate\": null}".formatted(areaId))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.dueDate").value((Object) null));
        }

        @Test
        @DisplayName("progress 欄位在沒有子任務時為 null（欄位存在）")
        void progressIsNullNotMissing() throws Exception {
            String body =
                    api.createGoal("{\"areaId\": %d, \"title\": \"A\"}".formatted(areaId))
                            .andReturn()
                            .getResponse()
                            .getContentAsString();

            assertThat(body).contains("\"progress\":null");
        }

        @Test
        @DisplayName("截止日格式錯誤回傳 400")
        void invalidDueDate() throws Exception {
            api.createGoal(
                            "{\"areaId\": %d, \"title\": \"A\", \"dueDate\": \"2027-02-30\"}"
                                    .formatted(areaId))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"))
                    .andExpect(jsonPath("$.message").value("預期截止日格式應為 YYYY-MM-DD"));
        }

        @Test
        @DisplayName("標題空白或超過 200 字回傳 400")
        void titleValidation() throws Exception {
            api.createGoal("{\"areaId\": %d, \"title\": \" \"}".formatted(areaId))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.message").value("目標標題必填"));
            api.createGoal(
                            "{\"areaId\": %d, \"title\": \"%s\"}".formatted(areaId, "x".repeat(201)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.message").value("目標標題不可超過 200 個字"));
        }

        @Test
        @DisplayName("領域不存在回傳 404 AREA_NOT_FOUND")
        void areaNotFound() throws Exception {
            api.createGoal("{\"areaId\": 99999, \"title\": \"A\"}")
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.code").value("AREA_NOT_FOUND"));
        }

        @Test
        @DisplayName("領域已隱藏回傳 409 AREA_HIDDEN")
        void areaHidden() throws Exception {
            api.hideArea(areaId);

            api.createGoal("{\"areaId\": %d, \"title\": \"A\"}".formatted(areaId))
                    .andExpect(status().isConflict())
                    .andExpect(jsonPath("$.code").value("AREA_HIDDEN"))
                    .andExpect(jsonPath("$.message").value("此領域已隱藏，無法新增目標"));
        }

        @Test
        @DisplayName("缺少所屬領域回傳 400")
        void areaIdRequired() throws Exception {
            api.createGoal("{\"title\": \"A\"}")
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.message").value("所屬領域必填"));
        }
    }

    @Nested
    @DisplayName("DELETE /api/goals/{id} 刪除")
    class Delete {

        @Test
        @DisplayName("回傳 204，子任務一併刪除；再刪一次回傳 404")
        void deletesWithSubtasks() throws Exception {
            long goalId = api.goal(areaId, "目標");
            api.subtask(goalId, "子任務一");
            api.subtask(goalId, "子任務二");

            mockMvc.perform(delete("/api/goals/{id}", goalId)).andExpect(status().isNoContent());

            assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM goal", Integer.class)).isZero();
            assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM subtask", Integer.class)).isZero();
            mockMvc.perform(delete("/api/goals/{id}", goalId))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.code").value("GOAL_NOT_FOUND"))
                    .andExpect(jsonPath("$.message").value("找不到此目標"));
        }
    }

    @Nested
    @DisplayName("POST /api/goals/{id}/archive 移至歷史成就")
    class Archive {

        @Test
        @DisplayName("COMPLETED 的目標可以封存：回傳 204，記錄 archived_at")
        void archivesCompletedGoal() throws Exception {
            long goalId = api.goal(areaId, "目標");
            api.setProgress(api.subtask(goalId, "子任務"), 100);

            mockMvc.perform(post("/api/goals/{id}/archive", goalId))
                    .andExpect(status().isNoContent());

            Map<String, Object> row =
                    jdbc.queryForMap("SELECT status, archived_at FROM goal WHERE id = ?", goalId);
            assertThat(row.get("status")).isEqualTo("ARCHIVED");
            assertThat(row.get("archived_at")).isNotNull();
        }

        @Test
        @DisplayName("未完成的目標回傳 409 GOAL_NOT_COMPLETED")
        void notCompleted() throws Exception {
            long empty = api.goal(areaId, "沒有子任務");
            long inProgress = api.goal(areaId, "進行中");
            api.setProgress(api.subtask(inProgress, "子任務"), 80);

            for (long goalId : new long[] {empty, inProgress}) {
                mockMvc.perform(post("/api/goals/{id}/archive", goalId))
                        .andExpect(status().isConflict())
                        .andExpect(jsonPath("$.code").value("GOAL_NOT_COMPLETED"))
                        .andExpect(jsonPath("$.message").value("目標尚未完成，無法移至歷史成就"));
            }
        }

        @Test
        @DisplayName("重複封存回傳 409 GOAL_ARCHIVED")
        void alreadyArchived() throws Exception {
            long goalId = api.goal(areaId, "目標");
            api.setProgress(api.subtask(goalId, "子任務"), 100);
            mockMvc.perform(post("/api/goals/{id}/archive", goalId))
                    .andExpect(status().isNoContent());

            mockMvc.perform(post("/api/goals/{id}/archive", goalId))
                    .andExpect(status().isConflict())
                    .andExpect(jsonPath("$.code").value("GOAL_ARCHIVED"))
                    .andExpect(jsonPath("$.message").value("此目標已在歷史成就中"));
        }

        @Test
        @DisplayName("已封存的目標仍可刪除")
        void archivedGoalCanBeDeleted() throws Exception {
            long goalId = api.goal(areaId, "目標");
            api.setProgress(api.subtask(goalId, "子任務"), 100);
            mockMvc.perform(post("/api/goals/{id}/archive", goalId))
                    .andExpect(status().isNoContent());

            mockMvc.perform(delete("/api/goals/{id}", goalId)).andExpect(status().isNoContent());
        }

        @Test
        @DisplayName("目標不存在回傳 404")
        void notFound() throws Exception {
            mockMvc.perform(post("/api/goals/99999/archive"))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.code").value("GOAL_NOT_FOUND"));
        }
    }

    @Test
    @DisplayName("路徑 id 不是正整數回傳 400")
    void invalidPathId() throws Exception {
        mockMvc.perform(delete("/api/goals/abc")).andExpect(status().isBadRequest());
        mockMvc.perform(delete("/api/goals/0"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"));
        mockMvc.perform(post("/api/goals/-1/archive"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"));
    }
}
