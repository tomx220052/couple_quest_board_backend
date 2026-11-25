package com.hannzer.cqb.goals;

import com.hannzer.cqb.goals.GoalView.SubtaskView;
import com.hannzer.cqb.goals.GoalView.UserRef;
import com.hannzer.cqb.users.AppUser;
import com.hannzer.cqb.users.AppUserRepository;
import java.time.format.DateTimeFormatter;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.stereotype.Component;

/** 把目標、子任務與建立者名稱組成 {@link GoalView} */
@Component
class GoalViews {

    private final AppUserRepository users;

    GoalViews(AppUserRepository users) {
        this.users = users;
    }

    /** subtasks 須為該目標的全部子任務，依建立順序 */
    GoalView of(Goal goal, List<Subtask> subtasks) {
        Set<Long> userIds = new HashSet<>();
        userIds.add(goal.getCreatedBy());
        subtasks.forEach(subtask -> userIds.add(subtask.getCreatedBy()));
        Map<Long, AppUser> userById =
                users.findAllById(userIds).stream()
                        .collect(Collectors.toMap(AppUser::getId, Function.identity()));

        List<SubtaskView> subtaskViews =
                subtasks.stream()
                        .map(
                                subtask ->
                                        new SubtaskView(
                                                subtask.getId(),
                                                subtask.getGoalId(),
                                                subtask.getTitle(),
                                                subtask.getProgress(),
                                                SubtaskState.of(subtask.getProgress()),
                                                userRef(userById, subtask.getCreatedBy())))
                        .toList();

        return new GoalView(
                goal.getId(),
                goal.getAreaId(),
                goal.getTitle(),
                goal.getDueDate() == null
                        ? null
                        : goal.getDueDate().format(DateTimeFormatter.ISO_LOCAL_DATE),
                goal.getStatus(),
                Progress.average(subtasks.stream().map(Subtask::getProgress).toList()),
                userRef(userById, goal.getCreatedBy()),
                subtaskViews);
    }

    private static UserRef userRef(Map<Long, AppUser> userById, long userId) {
        AppUser user = userById.get(userId);
        return new UserRef(userId, user == null ? "" : user.getDisplayName());
    }
}
