package com.hannzer.cqb.areas;

import jakarta.persistence.LockModeType;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;

public interface AreaRepository extends JpaRepository<Area, Long> {

    /** 全部領域（含已隱藏），依排序 */
    List<Area> findAllByOrderBySortOrderAscIdAsc();

    @Query("select coalesce(max(d.sortOrder), 0) from Area d")
    int findMaxSortOrder();

    /** 調整排序時鎖住全部領域列（SELECT … FOR UPDATE），避免兩人同時調整互相覆蓋 */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select d from Area d")
    List<Area> findAllForUpdate();
}
