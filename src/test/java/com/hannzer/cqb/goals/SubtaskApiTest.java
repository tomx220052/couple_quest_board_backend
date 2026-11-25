package com.hannzer.cqb.goals;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.hannzer.cqb.support.ApiFixtures;
import com.hannzer.cqb.support.IntegrationTest;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/** 子任務 API 與目標狀態自動轉換（規格書 4.2、4.3、第 6 節） */
@DisplayName("子任務 API")
class SubtaskApiTest extends IntegrationTest {

    private ApiFixtures api;
    private long goalId;

    @BeforeEach
    void setUpFixtures() throws Exception {
        api = new ApiFixtures(mockMvc);
        goalId = api.goal(api.area("健康"), "跑完半馬");
    }

    @Nested
    @DisplayName("POST /api/goals/{id}/subtasks 新增")
    class Create {

        @Test
        @DisplayName("回傳 201 與更新後的目標；新子任務進度 0、TODO，記錄建立者")
        void createsSubtask() throws Exception {
            Long devUserId =
                    jdbc.queryForObject(
                            "SELECT id FROM app_user WHERE email = ?", Long.class, DEV_EMAIL);

            api.createSubtask(goalId, "  排定訓練課表 ")
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.id").value(goalId))
                    .andExpect(jsonPath("$.status").value("ACTIVE"))
                    .andExpect(jsonPath("$.progress").value(0))
                    .andExpect(jsonPath("$.subtasks.length()").value(1))
                    .andExpect(jsonPath("$.subtasks[0].id").isNumber())
                    .andExpect(jsonPath("$.subtasks[0].goalId").value(goalId))
                    .andExpect(jsonPath("$.subtasks[0].title").value("排定訓練課表"))
                    .andExpect(jsonPath("$.subtasks[0].progress").value(0))
                    .andExpect(jsonPath("$.subtasks[0].state").value("TODO"))
                    .andExpect(jsonPath("$.subtasks[0].createdBy.id").value(devUserId))
                    .andExpect(jsonPath("$.subtasks[0].createdBy.displayName").value(DEV_NAME));
        }

        @Test
        @DisplayName("子任務依建立順序排列")
        void keepsCreationOrder() throws Exception {
            api.subtask(goalId, "一");
            api.subtask(goalId, "二");

            api.createSubtask(goalId, "三")
                    .andExpect(jsonPath("$.subtasks[*].title", contains("一", "二", "三")));
        }

        @Test
        @DisplayName("標題空白回傳 400")
        void blankTitle() throws Exception {
            api.createSubtask(goalId, " ")
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.message").value("子任務標題必填"));
        }

        @Test
        @DisplayName("目標不存在回傳 404 GOAL_NOT_FOUND")
        void goalNotFound() throws Exception {
            api.createSubtask(99999, "子任務")
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.code").value("GOAL_NOT_FOUND"));
        }
    }

    @Nested
    @DisplayName("PATCH /api/subtasks/{id} 修改進度")
    class UpdateProgress {

        @Test
        @DisplayName("回傳更新後的目標：目標進度 = 平均無條件捨去，state 由進度推導")
        void averagesProgress() throws Exception {
            long a = api.subtask(goalId, "A");
            long b = api.subtask(goalId, "B");
            api.subtask(goalId, "C");
            api.setProgress(a, 100);

            api.updateProgress(b, "40")
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.id").value(goalId))
                    .andExpect(jsonPath("$.progress").value(46))
                    .andExpect(jsonPath("$.subtasks[*].progress", contains(100, 40, 0)))
                    .andExpect(jsonPath("$.subtasks[*].state", contains("DONE", "IN_PROGRESS", "TODO")));
        }

        @Test
        @DisplayName("進度只接受 0／20／40／60／80／100，其他（含字串、缺少）回傳 400")
        void rejectsInvalidProgress() throws Exception {
            long subtaskId = api.subtask(goalId, "A");

            for (String value : new String[] {"50", "-20", "120", "\"20\"", "null"}) {
                api.updateProgress(subtaskId, value)
                        .andExpect(status().isBadRequest())
                        .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"))
                        .andExpect(jsonPath("$.message").value("進度只能是 0、20、40、60、80、100"));
            }
            assertThat(progressOf(subtaskId)).isZero();
        }

        @Test
        @DisplayName("子任務不存在回傳 404 SUBTASK_NOT_FOUND")
        void notFound() throws Exception {
            api.updateProgress(99999, "20")
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.code").value("SUBTASK_NOT_FOUND"))
                    .andExpect(jsonPath("$.message").value("找不到此子任務"));
        }
    }

    @Nested
    @DisplayName("DELETE /api/subtasks/{id} 刪除")
    class Delete {

        @Test
        @DisplayName("回傳更新後的目標；再刪一次回傳 404")
        void deletes() throws Exception {
            long a = api.subtask(goalId, "A");
            api.subtask(goalId, "B");

            mockMvc.perform(delete("/api/subtasks/{id}", a))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.id").value(goalId))
                    .andExpect(jsonPath("$.subtasks[*].title", contains("B")));
            mockMvc.perform(delete("/api/subtasks/{id}", a))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.code").value("SUBTASK_NOT_FOUND"));
        }
    }

    @Nested
    @DisplayName("目標狀態自動轉換")
    class StatusTransitions {

        @Test
        @DisplayName("全部子任務 100% → COMPLETED，並記錄 completed_at")
        void allDoneCompletes() throws Exception {
            long a = api.subtask(goalId, "A");
            long b = api.subtask(goalId, "B");
            api.setProgress(a, 100);

            api.updateProgress(b, "100")
                    .andExpect(jsonPath("$.status").value("COMPLETED"))
                    .andExpect(jsonPath("$.progress").value(100));
            assertThat(goalRow().get("completed_at")).isNotNull();
        }

        @Test
        @DisplayName("COMPLETED 後新增子任務 → ACTIVE，清除 completed_at")
        void addingSubtaskReactivates() throws Exception {
            api.setProgress(api.subtask(goalId, "A"), 100);

            api.createSubtask(goalId, "B")
                    .andExpect(jsonPath("$.status").value("ACTIVE"))
                    .andExpect(jsonPath("$.progress").value(50));
            assertThat(goalRow().get("completed_at")).isNull();
        }

        @Test
        @DisplayName("COMPLETED 後任一子任務降到 100% 以下 → ACTIVE，清除 completed_at")
        void loweringProgressReactivates() throws Exception {
            long a = api.subtask(goalId, "A");
            api.setProgress(a, 100);

            api.updateProgress(a, "80").andExpect(jsonPath("$.status").value("ACTIVE"));
            assertThat(goalRow().get("completed_at")).isNull();
        }

        @Test
        @DisplayName("刪除唯一未完成的子任務 → COMPLETED")
        void deletingLastUnfinishedCompletes() throws Exception {
            api.setProgress(api.subtask(goalId, "A"), 100);
            long b = api.subtask(goalId, "B");

            mockMvc.perform(delete("/api/subtasks/{id}", b))
                    .andExpect(jsonPath("$.status").value("COMPLETED"));
        }

        @Test
        @DisplayName("刪光子任務的目標不是 COMPLETED，進度為 null")
        void deletingAllSubtasksIsNotCompleted() throws Exception {
            long a = api.subtask(goalId, "A");
            api.setProgress(a, 100);

            mockMvc.perform(delete("/api/subtasks/{id}", a))
                    .andExpect(jsonPath("$.status").value("ACTIVE"))
                    .andExpect(jsonPath("$.progress").doesNotExist())
                    .andExpect(jsonPath("$.subtasks").isEmpty());
        }
    }

    @Nested
    @DisplayName("已移至歷史成就的目標")
    class ArchivedGoal {

        @Test
        @DisplayName("不可再新增、修改、刪除子任務（409 GOAL_ARCHIVED），資料不變")
        void subtasksAreFrozen() throws Exception {
            long a = api.subtask(goalId, "A");
            api.setProgress(a, 100);
            mockMvc.perform(post("/api/goals/{id}/archive", goalId))
                    .andExpect(status().isNoContent());

            api.createSubtask(goalId, "B")
                    .andExpect(status().isConflict())
                    .andExpect(jsonPath("$.code").value("GOAL_ARCHIVED"))
                    .andExpect(jsonPath("$.message").value("目標已移至歷史成就，無法再異動子任務"));
            api.updateProgress(a, "80")
                    .andExpect(status().isConflict())
                    .andExpect(jsonPath("$.code").value("GOAL_ARCHIVED"));
            mockMvc.perform(delete("/api/subtasks/{id}", a))
                    .andExpect(status().isConflict())
                    .andExpect(jsonPath("$.code").value("GOAL_ARCHIVED"));

            assertThat(progressOf(a)).isEqualTo(100);
            assertThat(goalRow().get("status")).isEqualTo("ARCHIVED");
        }
    }

    @Test
    @DisplayName("兩人同時把各自的子任務推到 100%，目標仍正確變為 COMPLETED")
    void concurrentUpdatesCompleteGoal() throws Exception {
        ExecutorService pool = Executors.newFixedThreadPool(2);
        try {
            for (int round = 0; round < 5; round++) {
                long goal = api.goal(api.area("併發 " + round), "目標 " + round);
                long a = api.subtask(goal, "A");
                long b = api.subtask(goal, "B");

                CountDownLatch start = new CountDownLatch(1);
                List<Future<Integer>> results = new ArrayList<>();
                for (long subtaskId : new long[] {a, b}) {
                    Callable<Integer> task =
                            () -> {
                                start.await();
                                return api.updateProgress(subtaskId, "100")
                                        .andReturn()
                                        .getResponse()
                                        .getStatus();
                            };
                    results.add(pool.submit(task));
                }
                start.countDown();
                for (Future<Integer> result : results) {
                    assertThat(result.get()).isEqualTo(200);
                }

                String status =
                        jdbc.queryForObject(
                                "SELECT status FROM goal WHERE id = ?", String.class, goal);
                assertThat(status).as("第 %d 輪", round + 1).isEqualTo("COMPLETED");
            }
        } finally {
            pool.shutdownNow();
        }
    }

    @Test
    @DisplayName("路徑 id 不是正整數回傳 400")
    void invalidPathId() throws Exception {
        mockMvc.perform(delete("/api/subtasks/0"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"));
        api.createSubtask(-1, "A")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"));
    }

    private Map<String, Object> goalRow() {
        return jdbc.queryForMap("SELECT status, completed_at FROM goal WHERE id = ?", goalId);
    }

    private int progressOf(long subtaskId) {
        return jdbc.queryForObject(
                "SELECT progress FROM subtask WHERE id = ?", Integer.class, subtaskId);
    }
}
