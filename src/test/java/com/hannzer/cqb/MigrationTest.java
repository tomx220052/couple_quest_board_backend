package com.hannzer.cqb;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.hannzer.cqb.support.IntegrationTest;
import java.time.LocalDateTime;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataAccessException;

@DisplayName("Flyway V1：規格書第 5 節資料表")
class MigrationTest extends IntegrationTest {

    @Test
    @DisplayName("建立 app_user、domain、goal、subtask 四張資料表")
    void createsTables() {
        var tables =
                jdbc.queryForList(
                        """
                        SELECT table_name FROM information_schema.tables
                        WHERE table_schema = DATABASE() AND table_name <> 'flyway_schema_history'
                        ORDER BY table_name
                        """,
                        String.class);

        assertThat(tables).containsExactly("app_user", "domain", "goal", "subtask");
    }

    @Test
    @DisplayName("子任務進度只接受 0／20／40／60／80／100，刪除目標會連帶刪除子任務")
    void subtaskProgressCheckAndCascade() {
        long goalId = insertGoal();

        insertSubtask(goalId, 40);
        assertThatThrownBy(() -> insertSubtask(goalId, 50))
                .isInstanceOf(DataAccessException.class)
                .hasStackTraceContaining("chk_subtask_progress");

        jdbc.update("DELETE FROM goal WHERE id = ?", goalId);
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM subtask", Integer.class)).isZero();
    }

    @Test
    @DisplayName("updated_at 由資料庫在更新時自動刷新")
    void updatedAtRefreshesOnUpdate() {
        insertGoal();
        jdbc.update("UPDATE domain SET updated_at = '2000-01-01 00:00:00'");

        jdbc.update("UPDATE domain SET name = '健康生活'");

        LocalDateTime updatedAt =
                jdbc.queryForObject("SELECT updated_at FROM domain", LocalDateTime.class);
        assertThat(updatedAt).isAfter(LocalDateTime.now().minusMinutes(1));
    }

    @Test
    @DisplayName("資料庫連線時區為 +08:00（Asia/Taipei）")
    void sessionTimeZoneIsTaipei() {
        String offset =
                jdbc.queryForObject(
                        "SELECT TIME_FORMAT(TIMEDIFF(NOW(), UTC_TIMESTAMP()), '%H:%i')",
                        String.class);

        assertThat(offset).isEqualTo("08:00");
    }

    private long insertGoal() {
        jdbc.update("INSERT INTO app_user (email, display_name) VALUES ('m@example.com', 'M')");
        jdbc.update(
                "INSERT INTO domain (name, sort_order, created_by)"
                        + " SELECT '健康', 1, id FROM app_user WHERE email = 'm@example.com'");
        jdbc.update(
                "INSERT INTO goal (domain_id, title, created_by)"
                        + " SELECT d.id, '目標', d.created_by FROM domain d");
        return jdbc.queryForObject("SELECT id FROM goal", Long.class);
    }

    private void insertSubtask(long goalId, int progress) {
        jdbc.update(
                "INSERT INTO subtask (goal_id, title, progress, created_by)"
                        + " SELECT id, '子任務', ?, created_by FROM goal WHERE id = ?",
                progress,
                goalId);
    }
}
