package com.hannzer.cqb.goals;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDate;
import java.time.LocalDateTime;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

/**
 * 目標：領域下想達成的成果（規格書 2.1）。目標與其子任務構成一個 aggregate，子任務的任何異動都先鎖住目標列。
 *
 * <p>建立後不提供編輯（規格書 2.2），只有狀態會變動。created_at、updated_at 由資料庫維護。
 */
@Entity
@Table(name = "goal")
public class Goal {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "area_id", nullable = false, updatable = false)
    private Long areaId;

    @Column(nullable = false, updatable = false)
    private String title;

    @Column(name = "due_date", updatable = false)
    private LocalDate dueDate;

    /** 資料表是 VARCHAR（規格書 5：不用 MySQL ENUM），明確指定避免 Hibernate 對應成原生 ENUM */
    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(nullable = false, length = 16)
    private GoalStatus status = GoalStatus.ACTIVE;

    @Column(name = "completed_at")
    private LocalDateTime completedAt;

    @Column(name = "archived_at")
    private LocalDateTime archivedAt;

    @Column(name = "created_by", nullable = false, updatable = false)
    private Long createdBy;

    /** 只用於排序查詢，由資料庫預設值寫入 */
    @Column(name = "created_at", insertable = false, updatable = false)
    private LocalDateTime createdAt;

    protected Goal() {}

    Goal(long areaId, String title, LocalDate dueDate, long createdBy) {
        this.areaId = areaId;
        this.title = title;
        this.dueDate = dueDate;
        this.createdBy = createdBy;
    }

    public Long getId() {
        return id;
    }

    public Long getAreaId() {
        return areaId;
    }

    public String getTitle() {
        return title;
    }

    public LocalDate getDueDate() {
        return dueDate;
    }

    public GoalStatus getStatus() {
        return status;
    }

    public Long getCreatedBy() {
        return createdBy;
    }

    /** 依子任務狀況重算狀態；進入 COMPLETED 記錄完成時間，回到 ACTIVE 時清除 */
    void recomputeStatus(int subtaskCount, Integer minProgress, LocalDateTime now) {
        GoalStatus next = GoalStatusPolicy.next(status, subtaskCount, minProgress);
        if (next == status) {
            return;
        }
        status = next;
        completedAt = next == GoalStatus.COMPLETED ? now : null;
    }

    /** 移至歷史成就；呼叫端須先確認目前為 COMPLETED */
    void archive(LocalDateTime now) {
        status = GoalStatus.ARCHIVED;
        archivedAt = now;
    }
}
