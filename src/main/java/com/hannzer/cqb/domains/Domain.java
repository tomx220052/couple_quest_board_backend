package com.hannzer.cqb.domains;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * 領域：人生面向，主畫面上的頁籤（規格書 2.1）。只能隱藏不能刪除（規格書 4.4）。
 *
 * <p>created_at、updated_at 由資料庫維護，不對應到欄位。
 */
@Entity
@Table(name = "domain")
public class Domain {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    @Column(name = "sort_order", nullable = false)
    private int sortOrder;

    @Column(nullable = false)
    private boolean hidden;

    @Column(name = "created_by", nullable = false, updatable = false)
    private Long createdBy;

    protected Domain() {}

    public Domain(String name, int sortOrder, long createdBy) {
        this.name = name;
        this.sortOrder = sortOrder;
        this.createdBy = createdBy;
    }

    public Long getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public int getSortOrder() {
        return sortOrder;
    }

    public boolean isHidden() {
        return hidden;
    }

    public Long getCreatedBy() {
        return createdBy;
    }

    void rename(String name) {
        this.name = name;
    }

    void moveTo(int sortOrder) {
        this.sortOrder = sortOrder;
    }

    void setHidden(boolean hidden) {
        this.hidden = hidden;
    }
}
