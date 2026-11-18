package com.hannzer.cqb.auth;

import com.hannzer.cqb.common.error.AppException;
import com.hannzer.cqb.common.error.ErrorCode;
import com.hannzer.cqb.users.AppUser;
import com.hannzer.cqb.users.AppUserRepository;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

/**
 * 所有 /api/**（/api/health 除外）都要先確認登入者：取得 email → 對應 app_user → 存入請求。
 *
 * <p>在 DispatcherServlet 內執行，丟出的 AppException 會由 GlobalExceptionHandler 轉成統一錯誤格式。
 */
@Component
class AuthInterceptor implements HandlerInterceptor {

    static final String CURRENT_USER_ATTRIBUTE = CurrentUser.class.getName();

    private static final Logger log = LoggerFactory.getLogger(AuthInterceptor.class);

    private final EmailResolver emailResolver;
    private final AppUserRepository users;

    AuthInterceptor(EmailResolver emailResolver, AppUserRepository users) {
        this.emailResolver = emailResolver;
        this.users = users;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        String email = emailResolver.resolve(request);
        AppUser user =
                users.findByEmail(email)
                        .orElseThrow(
                                () -> {
                                    log.warn("未登記的帳號嘗試存取：{}", email);
                                    return new AppException(
                                            HttpStatus.FORBIDDEN,
                                            ErrorCode.USER_NOT_REGISTERED,
                                            "此帳號沒有使用權限");
                                });
        request.setAttribute(
                CURRENT_USER_ATTRIBUTE,
                new CurrentUser(user.getId(), user.getEmail(), user.getDisplayName()));
        return true;
    }
}
