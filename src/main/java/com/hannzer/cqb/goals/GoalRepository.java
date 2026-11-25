package com.hannzer.cqb.goals;

import jakarta.persistence.LockModeType;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface GoalRepository extends JpaRepository<Goal, Long> {

    /** SELECT … FOR UPDATE 鎖住目標列：子任務異動與封存都先經過這裡，兩人同時操作時依序執行 */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select g from Goal g where g.id = :id")
    Optional<Goal> findByIdForUpdate(@Param("id") long id);

    /** 刪除目標；子任務由外鍵 ON DELETE CASCADE 一併刪除。回傳刪除筆數 */
    @Modifying
    @Query("delete from Goal g where g.id = :id")
    int deleteGoal(@Param("id") long id);
}
