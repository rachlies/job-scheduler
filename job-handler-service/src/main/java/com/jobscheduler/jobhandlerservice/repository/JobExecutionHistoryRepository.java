package com.jobscheduler.jobhandlerservice.repository;

import com.jobscheduler.jobhandlerservice.model.JobExecutionHistory;
import org.springframework.data.jpa.repository.JpaRepository;

public interface JobExecutionHistoryRepository extends JpaRepository<JobExecutionHistory, Long> {
}
