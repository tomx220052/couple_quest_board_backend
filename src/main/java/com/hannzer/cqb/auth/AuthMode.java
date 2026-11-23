package com.hannzer.cqb.auth;

/** 登入模式（規格書 7.3） */
public enum AuthMode {
    /** 正式：驗證 Cloudflare Access 加在請求上的 JWT */
    ACCESS,
    /** 開發：跳過驗證，所有請求視為固定使用者；僅限本機開發與測試 */
    DEV
}
