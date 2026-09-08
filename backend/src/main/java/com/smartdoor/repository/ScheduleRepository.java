package com.smartdoor.repository;

import com.smartdoor.domain.AccessSchedule;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import java.time.DayOfWeek;
import java.util.List;

public interface ScheduleRepository extends JpaRepository<AccessSchedule, String> {
    List<AccessSchedule> findAllByUserIdOrderByDayOfWeekAscStartTimeAsc(String userId);
    List<AccessSchedule> findAllByUserIdAndDayOfWeek(String userId, DayOfWeek dayOfWeek);
    @Modifying
    @Query("delete from AccessSchedule s where s.user.id = :userId")
    void deleteAllByUserId(String userId);
}
