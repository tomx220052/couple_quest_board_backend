package com.hannzer.cqb.areas;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import java.util.List;

/** 領域 API 的請求內容 */
final class AreaRequests {

    static final int NAME_MAX_LENGTH = 50;

    private AreaRequests() {}

    /** POST /api/areas、PATCH /api/areas/{id}；名稱先去除前後空白再驗證 */
    record Name(
            @NotEmpty(message = "領域名稱必填")
                    @Size(max = NAME_MAX_LENGTH, message = "領域名稱不可超過 50 個字")
                    String name) {

        Name {
            name = name == null ? null : name.strip();
        }
    }

    /** PUT /api/areas/order：完整的領域 id 順序 */
    record Order(
            @NotEmpty(message = "排序不可為空")
                    List<@NotNull(message = "領域 id 必填") @Positive(message = "領域 id 必須為正整數") Long>
                            ids) {}
}
