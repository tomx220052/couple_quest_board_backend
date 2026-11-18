package com.hannzer.cqb.auth;

import com.hannzer.cqb.common.error.AppException;
import jakarta.servlet.http.HttpServletRequest;

/**
 * 正式模式：從 Cloudflare Access 加在每個請求上的 JWT 取得 email（規格書 7.3）。
 *
 * <p>B0 只檢查 header 是否存在；簽章、簽發者、aud、有效期限的驗證在 B4 實作，在那之前一律拒絕。
 */
class AccessEmailResolver implements EmailResolver {

    static final String ACCESS_JWT_HEADER = "Cf-Access-Jwt-Assertion";

    @Override
    public String resolve(HttpServletRequest request) {
        String token = request.getHeader(ACCESS_JWT_HEADER);
        if (token == null || token.isBlank()) {
            throw AppException.unauthenticated("尚未登入");
        }
        // TODO(B4)：以 Access 團隊網域的公開金鑰驗證 JWT，取出 email
        throw AppException.unauthenticated("登入資訊無效，請重新整理頁面");
    }
}
