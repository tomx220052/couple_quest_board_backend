package com.hannzer.cqb.goals;

import static com.hannzer.cqb.goals.GoalStatus.ACTIVE;
import static com.hannzer.cqb.goals.GoalStatus.ARCHIVED;
import static com.hannzer.cqb.goals.GoalStatus.COMPLETED;
import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

/** 目標狀態規則（規格書 4.2）：純函式，不需要 Spring 與資料庫 */
@DisplayName("GoalStatusPolicy：目標狀態規則")
class GoalStatusPolicyTest {

    @ParameterizedTest(name = "{0}，{1} 個子任務、最低進度 {2} → {3}")
    @CsvSource(
            nullValues = "null",
            value = {
                // 沒有子任務不會進入 COMPLETED
                "ACTIVE,    0, null, ACTIVE",
                "COMPLETED, 0, null, ACTIVE",
                // 全部 100% → COMPLETED
                "ACTIVE,    1, 100,  COMPLETED",
                "ACTIVE,    3, 100,  COMPLETED",
                "COMPLETED, 2, 100,  COMPLETED",
                // 任一未滿 100% → ACTIVE
                "ACTIVE,    2, 80,   ACTIVE",
                "ACTIVE,    1, 0,    ACTIVE",
                "COMPLETED, 3, 80,   ACTIVE",
                "COMPLETED, 2, 0,    ACTIVE",
            })
    void next(GoalStatus current, int subtaskCount, Integer minProgress, GoalStatus expected) {
        assertThat(GoalStatusPolicy.next(current, subtaskCount, minProgress)).isEqualTo(expected);
    }

    @Test
    @DisplayName("ARCHIVED 為終點狀態，子任務怎麼變都不動")
    void archivedIsTerminal() {
        assertThat(GoalStatusPolicy.next(ARCHIVED, 0, null)).isEqualTo(ARCHIVED);
        assertThat(GoalStatusPolicy.next(ARCHIVED, 2, 100)).isEqualTo(ARCHIVED);
        assertThat(GoalStatusPolicy.next(ARCHIVED, 2, 40)).isEqualTo(ARCHIVED);
    }
}
