package com.hannzer.cqb.auth;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
class AuthController {

    /** GET /api/auth/me：目前登入者 */
    @GetMapping("/me")
    MeResponse me(CurrentUser user) {
        return new MeResponse(user.id(), user.email(), user.displayName());
    }

    record MeResponse(long id, String email, String displayName) {}
}
