package com.hannzer.cqb.domains;

/** GET /api/domains 與領域相關異動的回應（前端 DomainView） */
public record DomainView(long id, String name, int sortOrder, boolean hidden) {

    static DomainView from(Domain domain) {
        return new DomainView(domain.getId(), domain.getName(), domain.getSortOrder(), domain.isHidden());
    }
}
