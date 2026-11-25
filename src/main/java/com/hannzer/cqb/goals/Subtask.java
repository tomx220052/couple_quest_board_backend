package com.hannzer.cqb.goals;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

/** 子任務：達成目標的工作，各自有進度（規格書 2.1）。建立後只能修改進度。 */
@Entity
@Table(name = "subtask")
public class Subtask {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "goal_id", nullable = false, updatable = false)
    private Long goalId;

    @Column(nullable = false, updatable = false)
    private String title;

    /** 資料表為 TINYINT UNSIGNED，值域 0–100 */
    @JdbcTypeCode(SqlTypes.TINYINT)
    @Column(nullable = false)
    private int progress;

    @Column(name = "created_by", nullable = false, updatable = false)
    private Long createdBy;

    /** 只用於排序查詢，由資料庫預設值寫入 */
    @Column(name = "created_at", insertable = false, updatable = false)
    private LocalDateTime createdAt;

    protected Subtask() {}

    Subtask(long goalId, String title, long createdBy) {
        this.goalId = goalId;
        this.title = title;
        this.createdBy = createdBy;
    }

    public Long getId() {
        return id;
    }

    public Long getGoalId() {
        return goalId;
    }

    public String getTitle() {
        return title;
    }

    public int getProgress() {
        return progress;
    }

    public Long getCreatedBy() {
        return createdBy;
    }

    void setProgress(int progress) {
        this.progress = progress;
    }
}
