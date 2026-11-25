package com.hannzer.cqb.goals;

/** 目標狀態（規格書 4.2）；資料庫以 VARCHAR 存名稱 */
public enum GoalStatus {
    /** 進行中 */
    ACTIVE,
    /** 所有子任務皆 100% */
    COMPLETED,
    /** 已移至歷史成就；Phase one 的終點狀態 */
    ARCHIVED
}
