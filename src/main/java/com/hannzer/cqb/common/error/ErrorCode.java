package com.hannzer.cqb.common.error;

/** 錯誤代碼，與前端共用的 ERROR_CODES 一致（規格書第 6 節） */
public enum ErrorCode {
    // 400
    VALIDATION_FAILED,
    INVALID_AREA_ORDER,
    // 401／403
    UNAUTHENTICATED,
    USER_NOT_REGISTERED,
    // 404
    NOT_FOUND,
    AREA_NOT_FOUND,
    GOAL_NOT_FOUND,
    SUBTASK_NOT_FOUND,
    // 409
    AREA_NAME_DUPLICATED,
    AREA_HIDDEN,
    GOAL_NOT_COMPLETED,
    GOAL_ARCHIVED,
    // 500
    INTERNAL_ERROR
}
