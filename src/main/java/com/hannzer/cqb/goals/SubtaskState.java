package com.hannzer.cqb.goals;

/** 子任務狀態，由進度推導，不另存欄位（規格書 4.2） */
public enum SubtaskState {
    TODO,
    IN_PROGRESS,
    DONE;

    static SubtaskState of(int progress) {
        if (progress <= 0) {
            return TODO;
        }
        return progress >= Progress.MAX ? DONE : IN_PROGRESS;
    }
}
