package com.hannzer.cqb.goals;

import java.util.List;

/**
 * 目標的回應內容（後端企劃書 5.1）。
 *
 * @param dueDate YYYY-MM-DD；未填為 null
 * @param progress 子任務進度平均（無條件捨去）；沒有子任務時為 null
 */
public record GoalView(
        long id,
        long areaId,
        String title,
        String dueDate,
        GoalStatus status,
        Integer progress,
        UserRef createdBy,
        List<SubtaskView> subtasks) {

    public record SubtaskView(
            long id, long goalId, String title, int progress, SubtaskState state, UserRef createdBy) {}

    /** 建立者：只顯示名稱（規格書 2.2） */
    public record UserRef(long id, String displayName) {}
}
