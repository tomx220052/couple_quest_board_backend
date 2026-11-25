package com.hannzer.cqb.goals;

/** 依子任務狀況決定目標狀態（規格書 4.2）。純函式，子任務新增、刪除、改進度後呼叫。 */
final class GoalStatusPolicy {

    private GoalStatusPolicy() {}

    /**
     * @param current 目前狀態
     * @param subtaskCount 子任務數量
     * @param minProgress 子任務最低進度；沒有子任務時為 null
     * @return ARCHIVED 維持不變；至少一個子任務且全部 100% 為 COMPLETED；其餘為 ACTIVE
     */
    static GoalStatus next(GoalStatus current, int subtaskCount, Integer minProgress) {
        if (current == GoalStatus.ARCHIVED) {
            return current;
        }
        boolean allDone =
                subtaskCount > 0 && minProgress != null && minProgress == Progress.MAX;
        return allDone ? GoalStatus.COMPLETED : GoalStatus.ACTIVE;
    }
}
