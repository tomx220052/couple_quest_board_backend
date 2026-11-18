package com.hannzer.cqb.auth;

/** 目前登入者。Controller 方法宣告此型別的參數即可取得。 */
public record CurrentUser(long id, String email, String displayName) {}
