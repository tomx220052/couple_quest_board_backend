package com.hannzer.cqb.goals;

import com.hannzer.cqb.common.validation.IsoDate;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;

/** 目標與子任務 API 的請求內容；標題先去除前後空白再驗證 */
final class GoalRequests {

    static final int TITLE_MAX_LENGTH = 200;

    private GoalRequests() {}

    /** POST /api/goals */
    record CreateGoal(
            @NotNull(message = "所屬領域必填") @Positive(message = "所屬領域 id 必須為正整數") Long areaId,
            @NotEmpty(message = "目標標題必填")
                    @Size(max = TITLE_MAX_LENGTH, message = "目標標題不可超過 200 個字")
                    String title,
            @IsoDate(message = "預期截止日格式應為 YYYY-MM-DD") String dueDate) {

        CreateGoal {
            title = strip(title);
        }

        /** 驗證通過後才呼叫；未填時為 null */
        LocalDate dueDateValue() {
            return dueDate == null ? null : IsoDate.Validator.parse(dueDate);
        }
    }

    /** POST /api/goals/{id}/subtasks */
    record CreateSubtask(
            @NotEmpty(message = "子任務標題必填")
                    @Size(max = TITLE_MAX_LENGTH, message = "子任務標題不可超過 200 個字")
                    String title) {

        CreateSubtask {
            title = strip(title);
        }
    }

    /** PATCH /api/subtasks/{id}：只能修改進度 */
    record UpdateProgress(@ProgressValue Object progress) {

        /** 驗證通過後才呼叫 */
        int progressValue() {
            return (Integer) progress;
        }
    }

    private static String strip(String value) {
        return value == null ? null : value.strip();
    }
}
