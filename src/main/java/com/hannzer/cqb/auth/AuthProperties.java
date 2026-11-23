package com.hannzer.cqb.auth;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * 登入設定，對應環境變數 AUTH_MODE、DEV_USER_EMAIL、DEV_USER_NAME、ACCESS_TEAM_DOMAIN、ACCESS_AUD。
 *
 * @param mode 登入模式，預設 access
 * @param devUserEmail 開發模式的固定使用者 email（dev 模式必填）
 * @param devUserName 開發模式的固定使用者顯示名稱
 * @param accessTeamDomain Cloudflare Access 團隊網域，例如 xxx.cloudflareaccess.com（B4 使用）
 * @param accessAud Cloudflare Access 應用程式的 Audience Tag（B4 使用）
 */
@ConfigurationProperties("cqb.auth")
public record AuthProperties(
        AuthMode mode,
        String devUserEmail,
        String devUserName,
        String accessTeamDomain,
        String accessAud) {

    public AuthProperties {
        if (mode == null) {
            mode = AuthMode.ACCESS;
        }
    }
}
