package com.hannzer.cqb.auth;

import jakarta.servlet.http.HttpServletRequest;

/** 從請求取得登入者 email；無法確認身分時丟出 401 的 AppException */
public interface EmailResolver {

    String resolve(HttpServletRequest request);
}
