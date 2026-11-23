package com.hannzer.cqb.domains;

import com.hannzer.cqb.auth.CurrentUser;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/** 領域 API（規格書第 6 節） */
@RestController
@RequestMapping("/api/domains")
class DomainController {

    private static final String INVALID_ID = "id 必須為正整數";

    private final DomainService service;

    DomainController(DomainService service) {
        this.service = service;
    }

    /** 所有領域（含已隱藏），供「編輯領域」使用 */
    @GetMapping
    List<DomainView> list() {
        return service.list();
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    DomainView create(@Valid @RequestBody DomainRequests.Name body, CurrentUser user) {
        return service.create(body.name(), user.id());
    }

    /** 改名 */
    @PatchMapping("/{id}")
    DomainView rename(
            @PathVariable @Positive(message = INVALID_ID) long id,
            @Valid @RequestBody DomainRequests.Name body) {
        return service.rename(id, body.name());
    }

    /** 調整排序：傳入完整的領域 id 順序 */
    @PutMapping("/order")
    List<DomainView> reorder(@Valid @RequestBody DomainRequests.Order body) {
        return service.reorder(body.ids());
    }

    @PostMapping("/{id}/hide")
    DomainView hide(@PathVariable @Positive(message = INVALID_ID) long id) {
        return service.setHidden(id, true);
    }

    @PostMapping("/{id}/unhide")
    DomainView unhide(@PathVariable @Positive(message = INVALID_ID) long id) {
        return service.setHidden(id, false);
    }
}
