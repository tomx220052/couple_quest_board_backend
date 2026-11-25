package com.hannzer.cqb.goals;

import com.hannzer.cqb.auth.CurrentUser;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/** 目標與子任務 API（規格書第 6 節） */
@RestController
class GoalController {

    private static final String INVALID_ID = "id 必須為正整數";

    private final GoalService service;

    GoalController(GoalService service) {
        this.service = service;
    }

    @PostMapping("/api/goals")
    @ResponseStatus(HttpStatus.CREATED)
    GoalView createGoal(@Valid @RequestBody GoalRequests.CreateGoal body, CurrentUser user) {
        return service.createGoal(body.areaId(), body.title(), body.dueDateValue(), user.id());
    }

    /** 刪除目標（連同子任務） */
    @DeleteMapping("/api/goals/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    void deleteGoal(@PathVariable @Positive(message = INVALID_ID) long id) {
        service.deleteGoal(id);
    }

    /** 移至歷史成就（僅限 COMPLETED） */
    @PostMapping("/api/goals/{id}/archive")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    void archiveGoal(@PathVariable @Positive(message = INVALID_ID) long id) {
        service.archiveGoal(id);
    }

    /** 新增子任務，回傳更新後的目標 */
    @PostMapping("/api/goals/{id}/subtasks")
    @ResponseStatus(HttpStatus.CREATED)
    GoalView createSubtask(
            @PathVariable @Positive(message = INVALID_ID) long id,
            @Valid @RequestBody GoalRequests.CreateSubtask body,
            CurrentUser user) {
        return service.createSubtask(id, body.title(), user.id());
    }

    /** 只能修改進度，回傳更新後的目標 */
    @PatchMapping("/api/subtasks/{id}")
    GoalView updateSubtask(
            @PathVariable @Positive(message = INVALID_ID) long id,
            @Valid @RequestBody GoalRequests.UpdateProgress body) {
        return service.updateProgress(id, body.progressValue());
    }

    /** 刪除子任務，回傳更新後的目標 */
    @DeleteMapping("/api/subtasks/{id}")
    GoalView deleteSubtask(@PathVariable @Positive(message = INVALID_ID) long id) {
        return service.deleteSubtask(id);
    }
}
