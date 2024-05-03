package com.jobscheduler.jobexecutorservice.repository;

import com.jobscheduler.jobexecutorservice.Status;
import com.jobscheduler.jobexecutorservice.model.JobExecutionHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface JobExecutionHistoryRepository extends JpaRepository<JobExecutionHistory, Long> {
    @Query("SELECT jeh.task_id FROM JobExecutionHistory jeh WHERE jeh.task_id IN ?1 AND jeh.status = ?2")
    List<Long> fetchExpiredTasksByStatusAndIds(List<Long> taskIds, Status status);

}
