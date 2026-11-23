package com.hannzer.cqb.auth;

import jakarta.servlet.http.HttpServletRequest;

/** 開發模式：所有請求都視為固定使用者 */
class DevEmailResolver implements EmailResolver {

    private final String email;

    DevEmailResolver(String email) {
        this.email = email;
    }

    @Override
    public String resolve(HttpServletRequest request) {
        return email;
    }
}
