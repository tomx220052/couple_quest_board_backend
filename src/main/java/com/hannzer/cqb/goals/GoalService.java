package com.hannzer.cqb.goals;

import com.hannzer.cqb.areas.Area;
import com.hannzer.cqb.areas.AreaService;
import com.hannzer.cqb.common.error.AppException;
import com.hannzer.cqb.common.error.ErrorCode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

/**
 * 目標與子任務（規格書 4.2–4.4）。
 *
 * <p>併發規則：子任務的新增、改進度、刪除與目標封存，都在 READ COMMITTED 交易內先以 SELECT … FOR UPDATE
 * 鎖住目標列，再讀取子任務並重算狀態。兩人同時異動同一目標時會依序執行，且後執行者一定看得到前者已提交的結果。
 * （MySQL 預設的 REPEATABLE READ 在取得鎖之前若已做過一般查詢，之後的查詢會沿用舊快照，可能把狀態算錯。）
 */
@Service
@Transactional(isolation = Isolation.READ_COMMITTED)
public class GoalService {

    private final GoalRepository goals;
    private final SubtaskRepository subtasks;
    private final AreaService areas;
    private final GoalViews views;

    GoalService(
            GoalRepository goals, SubtaskRepository subtasks, AreaService areas, GoalViews views) {
        this.goals = goals;
        this.subtasks = subtasks;
        this.areas = areas;
        this.views = views;
    }

    // ---------- 目標 ----------

    /** 新增目標：所屬領域須存在且未隱藏 */
    public GoalView createGoal(long areaId, String title, LocalDate dueDate, long userId) {
        Area area = areas.getArea(areaId);
        if (area.isHidden()) {
            throw new AppException(
                    HttpStatus.CONFLICT, ErrorCode.AREA_HIDDEN, "此領域已隱藏，無法新增目標");
        }
        Goal goal = goals.save(new Goal(areaId, title, dueDate, userId));
        return views.of(goal, List.of());
    }

    /** 刪除目標，子任務由外鍵 ON DELETE CASCADE 一併刪除（規格書 4.4） */
    public void deleteGoal(long goalId) {
        if (goals.deleteGoal(goalId) == 0) {
            throw goalNotFound();
        }
    }

    /** 移至歷史成就：僅限 COMPLETED（規格書 4.2） */
    public void archiveGoal(long goalId) {
        Goal goal = goals.findByIdForUpdate(goalId).orElseThrow(GoalService::goalNotFound);
        if (goal.getStatus() == GoalStatus.ARCHIVED) {
            throw new AppException(HttpStatus.CONFLICT, ErrorCode.GOAL_ARCHIVED, "此目標已在歷史成就中");
        }
        if (goal.getStatus() != GoalStatus.COMPLETED) {
            throw new AppException(
                    HttpStatus.CONFLICT, ErrorCode.GOAL_NOT_COMPLETED, "目標尚未完成，無法移至歷史成就");
        }
        goal.archive(LocalDateTime.now());
    }

    // ---------- 子任務 ----------

    /** 新增子任務（進度 0%），回傳更新後的目標 */
    public GoalView createSubtask(long goalId, String title, long userId) {
        Goal goal = goals.findByIdForUpdate(goalId).orElseThrow(GoalService::goalNotFound);
        requireNotArchived(goal);
        subtasks.save(new Subtask(goalId, title, userId));
        return recomputeAndView(goal);
    }

    /** 修改進度，回傳更新後的目標 */
    public GoalView updateProgress(long subtaskId, int progress) {
        Goal goal = lockGoalOf(subtaskId);
        Subtask subtask = subtasks.findById(subtaskId).orElseThrow(GoalService::subtaskNotFound);
        requireNotArchived(goal);
        subtask.setProgress(progress);
        return recomputeAndView(goal);
    }

    /** 刪除子任務，回傳更新後的目標 */
    public GoalView deleteSubtask(long subtaskId) {
        Goal goal = lockGoalOf(subtaskId);
        Subtask subtask = subtasks.findById(subtaskId).orElseThrow(GoalService::subtaskNotFound);
        requireNotArchived(goal);
        subtasks.delete(subtask);
        return recomputeAndView(goal);
    }

    /**
     * 先鎖子任務所屬的目標，之後才讀取子任務本身：所有子任務異動都依「目標 → 子任務」的順序取得鎖。
     * 第一步只查目標 id，不把子任務載入 persistence context，鎖住後讀到的子任務才是最新的。
     */
    private Goal lockGoalOf(long subtaskId) {
        long goalId = subtasks.findGoalIdById(subtaskId).orElseThrow(GoalService::subtaskNotFound);
        // 目標在這之間被刪除時，子任務也已一併刪除
        return goals.findByIdForUpdate(goalId).orElseThrow(GoalService::subtaskNotFound);
    }

    /** 重新讀取子任務（查詢前會先 flush 本交易的異動），重算目標狀態並組成回應 */
    private GoalView recomputeAndView(Goal goal) {
        List<Subtask> current = subtasks.findByGoalIdOrderByCreatedAtAscIdAsc(goal.getId());
        Integer minProgress = current.stream().map(Subtask::getProgress).min(Integer::compare).orElse(null);
        goal.recomputeStatus(current.size(), minProgress, LocalDateTime.now());
        return views.of(goal, current);
    }

    private static void requireNotArchived(Goal goal) {
        if (goal.getStatus() == GoalStatus.ARCHIVED) {
            throw new AppException(
                    HttpStatus.CONFLICT, ErrorCode.GOAL_ARCHIVED, "目標已移至歷史成就，無法再異動子任務");
        }
    }

    private static AppException goalNotFound() {
        return new AppException(HttpStatus.NOT_FOUND, ErrorCode.GOAL_NOT_FOUND, "找不到此目標");
    }

    private static AppException subtaskNotFound() {
        return new AppException(HttpStatus.NOT_FOUND, ErrorCode.SUBTASK_NOT_FOUND, "找不到此子任務");
    }
}
