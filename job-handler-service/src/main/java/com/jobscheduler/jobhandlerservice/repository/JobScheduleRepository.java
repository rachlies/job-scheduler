package com.jobscheduler.jobhandlerservice.repository;

import com.jobscheduler.jobhandlerservice.model.JobSchedule;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface JobScheduleRepository extends JpaRepository<JobSchedule, Long> {

    @Query("SELECT js FROM JobSchedule js WHERE js.execution_time = ?1")
    List<JobSchedule> findAllByExecutionTime(Long execution_time);

    @Query("SELECT js FROM JobSchedule js WHERE js.task_id = ?1")
    JobSchedule findByTaskId(Long task_id);

    @Query("SELECT js FROM JobSchedule js WHERE js.execution_time <= ?1")
    List<JobSchedule> findAllExpiredSchedules(Long execution_time);
}
