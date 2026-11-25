package com.hannzer.cqb.goals;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface SubtaskRepository extends JpaRepository<Subtask, Long> {

    /** 目標的子任務，依建立順序 */
    List<Subtask> findByGoalIdOrderByCreatedAtAscIdAsc(long goalId);

    /** 只取所屬目標 id，不把子任務載入 persistence context，鎖住目標後再重新讀取子任務 */
    @Query("select s.goalId from Subtask s where s.id = :id")
    Optional<Long> findGoalIdById(@Param("id") long id);
}
