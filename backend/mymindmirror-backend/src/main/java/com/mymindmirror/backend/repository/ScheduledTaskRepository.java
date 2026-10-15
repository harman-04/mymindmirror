package com.mymindmirror.backend.repository;

import com.mymindmirror.backend.model.ScheduledTask;
import com.mymindmirror.backend.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public interface ScheduledTaskRepository extends JpaRepository<ScheduledTask, UUID> {

    List<ScheduledTask> findByUser(User user);

    List<ScheduledTask> findByUserAndScheduledDateBetween(User user, LocalDate start, LocalDate end);

    void deleteByUser(User user);

    List<ScheduledTask> findByCustomTaskId(UUID customTaskId);

    @Modifying
    @Query("DELETE FROM ScheduledTask st WHERE st.user = :user AND st.completed = false")
    void deleteIncompleteByUser(@Param("user") User user);

    @Modifying
    @Query("DELETE FROM ScheduledTask st WHERE st.user = :user AND st.completed = false AND st.scheduledDate = :today")
    void deleteIncompleteTodayByUser(@Param("user") User user, @Param("today") LocalDate today);
}