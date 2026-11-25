package com.hannzer.cqb.support;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.jayway.jsonpath.JsonPath;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

/** 透過 API 建立測試資料。測試用標題不含引號與反斜線，直接組 JSON。 */
public final class ApiFixtures {

    private final MockMvc mockMvc;

    public ApiFixtures(MockMvc mockMvc) {
        this.mockMvc = mockMvc;
    }

    public long area(String name) throws Exception {
        return idOf(
                postJson("/api/areas", "{\"name\": \"" + name + "\"}").andExpect(status().isCreated()));
    }

    public void hideArea(long areaId) throws Exception {
        mockMvc.perform(post("/api/areas/{id}/hide", areaId)).andExpect(status().isOk());
    }

    public long goal(long areaId, String title) throws Exception {
        return idOf(createGoal("{\"areaId\": %d, \"title\": \"%s\"}".formatted(areaId, title))
                .andExpect(status().isCreated()));
    }

    public ResultActions createGoal(String json) throws Exception {
        return postJson("/api/goals", json);
    }

    /** 新增子任務，回傳新子任務的 id（回應是更新後的目標，取最後一個子任務） */
    public long subtask(long goalId, String title) throws Exception {
        String body =
                createSubtask(goalId, title)
                        .andExpect(status().isCreated())
                        .andReturn()
                        .getResponse()
                        .getContentAsString();
        return ((Number) JsonPath.read(body, "$.subtasks[-1].id")).longValue();
    }

    public ResultActions createSubtask(long goalId, String title) throws Exception {
        return postJson("/api/goals/" + goalId + "/subtasks", "{\"title\": \"" + title + "\"}");
    }

    public ResultActions updateProgress(long subtaskId, String progressJson) throws Exception {
        return mockMvc.perform(
                patch("/api/subtasks/{id}", subtaskId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"progress\": " + progressJson + "}"));
    }

    public void setProgress(long subtaskId, int progress) throws Exception {
        updateProgress(subtaskId, String.valueOf(progress)).andExpect(status().isOk());
    }

    public ResultActions postJson(String path, String json) throws Exception {
        return mockMvc.perform(post(path).contentType(MediaType.APPLICATION_JSON).content(json));
    }

    public static long idOf(ResultActions result) throws Exception {
        String body = result.andReturn().getResponse().getContentAsString();
        return ((Number) JsonPath.read(body, "$.id")).longValue();
    }
}
