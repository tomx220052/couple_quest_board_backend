package com.hannzer.cqb.auth;

import com.hannzer.cqb.common.error.AppException;
import org.springframework.core.MethodParameter;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;

/** 讓 Controller 方法直接宣告 {@link CurrentUser} 參數 */
@Component
class CurrentUserArgumentResolver implements HandlerMethodArgumentResolver {

    @Override
    public boolean supportsParameter(MethodParameter parameter) {
        return CurrentUser.class.equals(parameter.getParameterType());
    }

    @Override
    public Object resolveArgument(
            MethodParameter parameter,
            ModelAndViewContainer mavContainer,
            NativeWebRequest webRequest,
            WebDataBinderFactory binderFactory) {
        Object user =
                webRequest.getAttribute(
                        AuthInterceptor.CURRENT_USER_ATTRIBUTE, RequestAttributes.SCOPE_REQUEST);
        if (user == null) {
            throw AppException.unauthenticated("尚未登入");
        }
        return user;
    }
}
