package com.hannzer.cqb.goals;

import java.util.Collection;
import java.util.Set;

/** 進度規則（規格書 4.3） */
final class Progress {

    static final int MAX = 100;

    /** 子任務進度可選值，與資料庫 chk_subtask_progress 一致 */
    static final Set<Integer> ALLOWED = Set.of(0, 20, 40, 60, 80, 100);

    private Progress() {}

    static boolean isAllowed(int value) {
        return ALLOWED.contains(value);
    }

    /** 目標進度 = 子任務進度平均（不加權、無條件捨去）；沒有子任務時為 null */
    static Integer average(Collection<Integer> subtaskProgress) {
        if (subtaskProgress.isEmpty()) {
            return null;
        }
        int sum = subtaskProgress.stream().mapToInt(Integer::intValue).sum();
        return sum / subtaskProgress.size();
    }
}
