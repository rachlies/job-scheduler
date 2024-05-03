package com.jobscheduler.jobexecutorservice.repository;

import com.jobscheduler.jobexecutorservice.model.JobSchedule;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface JobScheduleRepository extends JpaRepository<JobSchedule, Long> {

    @Query("SELECT js FROM JobSchedule js WHERE js.execution_time = ?1")
    List<JobSchedule> findAllByExecutionTime(Long execution_time);

    @Query("SELECT js FROM JobSchedule js WHERE js.execution_time <= ?1")
    List<JobSchedule> findAllExpiredSchedules(Long execution_time);
}
