package com.jobscheduler.jobhandlerservice.repository;

import com.jobscheduler.jobhandlerservice.model.Job;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface JobRepository extends JpaRepository<Job, Long> {
}
