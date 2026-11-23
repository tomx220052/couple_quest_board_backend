package com.hannzer.cqb.domains;

import com.hannzer.cqb.common.error.AppException;
import com.hannzer.cqb.common.error.ErrorCode;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** 領域：新增、改名、排序、隱藏／恢復顯示（規格書 4.4、4.5） */
@Service
@Transactional
public class DomainService {

    /** 資料庫的領域名稱唯一限制（V1__init.sql） */
    private static final String NAME_UNIQUE_CONSTRAINT = "uk_domain_name";

    private final DomainRepository domains;

    DomainService(DomainRepository domains) {
        this.domains = domains;
    }

    /** 全部領域（含已隱藏），依排序 */
    @Transactional(readOnly = true)
    public List<DomainView> list() {
        return domains.findAllByOrderBySortOrderAscIdAsc().stream().map(DomainView::from).toList();
    }

    /** 新增領域，排在最後 */
    public DomainView create(String name, long userId) {
        Domain domain = new Domain(name, domains.findMaxSortOrder() + 1, userId);
        saveAndFlush(domain);
        return DomainView.from(domain);
    }

    public DomainView rename(long id, String name) {
        Domain domain = get(id);
        domain.rename(name);
        saveAndFlush(domain);
        return DomainView.from(domain);
    }

    /** 調整排序：ids 必須剛好是全部領域（含已隱藏）的 id，各出現一次；依序重寫為 1、2、3… */
    public List<DomainView> reorder(List<Long> ids) {
        Map<Long, Domain> existing =
                domains.findAllForUpdate().stream()
                        .collect(Collectors.toMap(Domain::getId, Function.identity()));

        boolean complete =
                ids.size() == existing.size()
                        && new HashSet<>(ids).size() == ids.size()
                        && existing.keySet().containsAll(ids);
        if (!complete) {
            throw new AppException(
                    HttpStatus.BAD_REQUEST,
                    ErrorCode.INVALID_DOMAIN_ORDER,
                    "排序必須包含所有領域且不可重複，請重新整理後再試");
        }

        for (int i = 0; i < ids.size(); i++) {
            existing.get(ids.get(i)).moveTo(i + 1);
        }
        return list();
    }

    /** 隱藏或恢復顯示；其下目標與子任務原封不動，排序位置也保留（規格書 4.4、4.5） */
    public DomainView setHidden(long id, boolean hidden) {
        Domain domain = get(id);
        domain.setHidden(hidden);
        return DomainView.from(domain);
    }

    private Domain get(long id) {
        return domains
                .findById(id)
                .orElseThrow(
                        () ->
                                new AppException(
                                        HttpStatus.NOT_FOUND,
                                        ErrorCode.DOMAIN_NOT_FOUND,
                                        "找不到此領域"));
    }

    /** 立即寫入，讓名稱重複在這裡就被偵測到並轉成 409，而不是等到交易提交 */
    private void saveAndFlush(Domain domain) {
        try {
            domains.saveAndFlush(domain);
        } catch (DataIntegrityViolationException e) {
            if (isNameDuplicated(e)) {
                throw new AppException(
                        HttpStatus.CONFLICT, ErrorCode.DOMAIN_NAME_DUPLICATED, "領域名稱已存在");
            }
            throw e;
        }
    }

    private static boolean isNameDuplicated(DataIntegrityViolationException e) {
        for (Throwable t = e; t != null; t = t.getCause()) {
            String message = t.getMessage();
            if (message != null && message.contains(NAME_UNIQUE_CONSTRAINT)) {
                return true;
            }
        }
        return false;
    }
}
