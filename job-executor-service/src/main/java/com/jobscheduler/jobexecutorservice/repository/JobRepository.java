package com.jobscheduler.jobexecutorservice.repository;

import com.jobscheduler.jobexecutorservice.model.Job;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface JobRepository extends JpaRepository<Job, Long> {
}
