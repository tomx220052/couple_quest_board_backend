package com.hannzer.cqb.goals;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

/** 進度計算與子任務 state（規格書 4.2、4.3） */
@DisplayName("Progress：進度計算")
class ProgressTest {

    @Test
    @DisplayName("目標進度 = 子任務進度平均，無條件捨去")
    void averageFloors() {
        assertThat(Progress.average(List.of(100, 40, 0))).isEqualTo(46);
        assertThat(Progress.average(List.of(100, 100, 20))).isEqualTo(73);
        assertThat(Progress.average(List.of(20))).isEqualTo(20);
        assertThat(Progress.average(List.of(100, 100))).isEqualTo(100);
        assertThat(Progress.average(List.of(0, 0))).isZero();
    }

    @Test
    @DisplayName("沒有子任務時目標進度為 null")
    void averageOfNothingIsNull() {
        assertThat(Progress.average(List.of())).isNull();
    }

    @ParameterizedTest(name = "{0} 可以")
    @ValueSource(ints = {0, 20, 40, 60, 80, 100})
    @DisplayName("進度只接受 0／20／40／60／80／100")
    void allowedValues(int value) {
        assertThat(Progress.isAllowed(value)).isTrue();
    }

    @ParameterizedTest(name = "{0} 不行")
    @ValueSource(ints = {-20, -1, 1, 10, 50, 99, 101, 120})
    @DisplayName("其他數值都不接受")
    void disallowedValues(int value) {
        assertThat(Progress.isAllowed(value)).isFalse();
    }

    @ParameterizedTest(name = "進度 {0} → {1}")
    @CsvSource({"0, TODO", "20, IN_PROGRESS", "40, IN_PROGRESS", "60, IN_PROGRESS", "80, IN_PROGRESS", "100, DONE"})
    @DisplayName("子任務 state 由進度推導")
    void subtaskState(int progress, SubtaskState expected) {
        assertThat(SubtaskState.of(progress)).isEqualTo(expected);
    }
}
