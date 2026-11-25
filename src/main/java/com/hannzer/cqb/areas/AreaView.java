package com.hannzer.cqb.areas;

/** GET /api/areas 與領域相關異動的回應（前端 AreaView） */
public record AreaView(long id, String name, int sortOrder, boolean hidden) {

    static AreaView from(Area area) {
        return new AreaView(area.getId(), area.getName(), area.getSortOrder(), area.isHidden());
    }
}
