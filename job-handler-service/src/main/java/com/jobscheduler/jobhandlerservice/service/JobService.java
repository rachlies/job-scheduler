package com.jobscheduler.jobhandlerservice.service;

import com.jobscheduler.jobhandlerservice.model.Dto.JobResponse;
import com.jobscheduler.jobhandlerservice.model.JobSchedule;
import com.jobscheduler.jobhandlerservice.repository.JobExecutionHistoryRepository;
import com.jobscheduler.jobhandlerservice.repository.JobRepository;
import com.jobscheduler.jobhandlerservice.repository.JobScheduleRepository;
import com.jobscheduler.jobhandlerservice.model.Job;
import com.jobscheduler.jobhandlerservice.model.JobExecutionHistory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Optional;

@Service
public class JobService {
    private final JobRepository jobRepository;
    private final JobScheduleRepository jobScheduleRepository;
    private final JobExecutionHistoryRepository jobExecutionHistoryRepository;

    @Autowired
    public JobService(JobRepository jobRepository, JobScheduleRepository jobScheduleRepository, JobExecutionHistoryRepository jobExecutionHistoryRepository) {
        this.jobRepository = jobRepository;
        this.jobScheduleRepository = jobScheduleRepository;
        this.jobExecutionHistoryRepository = jobExecutionHistoryRepository;
    }

    public JobResponse createJob(Job jobRequest) {
        Job job = jobRepository.save(jobRequest);

        long executionTime = LocalDateTime.now()
                .plusHours(job.getHours())
                .plusMinutes(job.getMinutes())
                .plusSeconds(job.getSeconds())
                .atZone(ZoneOffset.systemDefault())
                .toInstant()
                .getEpochSecond();

        JobSchedule jobSchedule = new JobSchedule();
        jobSchedule.setExecution_time(executionTime);
        jobSchedule.setTask_id(job.getId());

        jobScheduleRepository.save(jobSchedule);

        JobExecutionHistory jobExecutionHistory = new JobExecutionHistory();
        jobExecutionHistory.setTask_id(job.getId());

        jobExecutionHistoryRepository.save(jobExecutionHistory);

        return new JobResponse(job.getId(), Math.max(0, executionTime - getCurrentTime()));
    }

    public JobResponse getJobById(Long id) {
        // Leverage Optional for null handling
        Optional<Job> optionalJob = jobRepository.findById(id);
        // Return null or create JobResponse if Job exists
        return optionalJob.map(job -> {
            JobSchedule jobSchedule = jobScheduleRepository.findByTaskId(job.getId());
            return new JobResponse(job.getId(), Math.max(0, jobSchedule.getExecution_time() - getCurrentTime()));
        }).orElse(null);
    }

    private static Long getCurrentTime() {
        return LocalDateTime.now()
                .atZone(ZoneOffset.systemDefault())
                .toInstant()
                .getEpochSecond();
    }
}

