package com.hannzer.cqb.common.error;

/** 錯誤回應格式：{ "code": "DOMAIN_NAME_DUPLICATED", "message": "領域名稱已存在" } */
public record ApiError(String code, String message) {

    public static ApiError of(ErrorCode code, String message) {
        return new ApiError(code.name(), message);
    }
}
