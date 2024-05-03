package com.jobscheduler.jobexecutorservice.service;

import com.jobscheduler.jobexecutorservice.repository.JobRepository;
import com.jobscheduler.jobexecutorservice.repository.JobScheduleRepository;
import com.jobscheduler.jobexecutorservice.Status;
import com.jobscheduler.jobexecutorservice.model.JobExecutionHistory;
import com.jobscheduler.jobexecutorservice.model.JobSchedule;
import com.jobscheduler.jobexecutorservice.repository.JobExecutionHistoryRepository;
import jakarta.annotation.PostConstruct;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.HashSet;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;


@Service
@EnableScheduling
public class JobService {
    private final String LOCK_KEY_PREFIX = "job_update_lock_";
    private final int LOCK_ACQUIRE_WAIT_TIME = 10;
    private final TimeUnit LOCK_ACQUIRE_WAIT_TIME_UNIT = TimeUnit.MILLISECONDS;
    private final RestTemplate restTemplate;

    @Value("${ID}")
    private String EXECUTOR_ID;

    private final JobRepository jobRepository;
    private final JobScheduleRepository jobScheduleRepository;
    private final JobExecutionHistoryRepository jobExecutionHistoryRepository;

    private final RedissonClient redissonClient;

    @Autowired
    public JobService(RestTemplate restTemplate,
                      JobRepository jobRepository,
                      JobScheduleRepository jobScheduleRepository,
                      JobExecutionHistoryRepository jobExecutionHistoryRepository,
                      RedissonClient redissonClient) {
        this.restTemplate = restTemplate;
        this.jobScheduleRepository = jobScheduleRepository;
        this.jobExecutionHistoryRepository = jobExecutionHistoryRepository;
        this.redissonClient = redissonClient;
        this.jobRepository = jobRepository;
    }

    @PostConstruct
    public void init() {
        runExpiredJob();
    }


    @Scheduled(cron = "${CRON_EXPRESSION}") // Run every minute
    public void fetchAndPrintJobIds() {
        System.out.println("Scheduler running and going to fetch db records...");

        List<JobSchedule> jobSchedules = jobScheduleRepository.findAllByExecutionTime(getCurrentTime());
        System.out.println("Count of the schedules is : " + jobSchedules.size());

        jobSchedules.forEach(this::executeJob);
    }

    private static Long getCurrentTime() {
        return LocalDateTime.now()
                .atZone(ZoneOffset.systemDefault())
                .toInstant()
                .getEpochSecond();
    }

    private void runExpiredJob() {
        List<JobSchedule> expiredSchedules = jobScheduleRepository.findAllExpiredSchedules(getCurrentTime());
        HashSet<Long> pendingTaskIds = new HashSet<>(
                jobExecutionHistoryRepository
                        .fetchExpiredTasksByStatusAndIds(
                                expiredSchedules.stream().map(JobSchedule::getTask_id).toList(), Status.PENDING));
        expiredSchedules = expiredSchedules.stream()
                .filter(jobSchedule -> pendingTaskIds.contains(jobSchedule.getTask_id())).toList();

        System.out.println("Count of the expired pending task schedules is : " + expiredSchedules.size());
        expiredSchedules.forEach(this::executeJob);
    }

    private void executeJob(JobSchedule jobSchedule) {
        String url = jobRepository.findById(jobSchedule.getTask_id()).orElseThrow().getUrl();
        String lockName = LOCK_KEY_PREFIX + jobSchedule.getId();

        // consider the configuration of TTL
        RLock lock = redissonClient.getLock(lockName);

        try {
            boolean acquired = lock.tryLock(LOCK_ACQUIRE_WAIT_TIME, LOCK_ACQUIRE_WAIT_TIME_UNIT);
            if (acquired) {
                JobExecutionHistory history =
                        jobExecutionHistoryRepository.findById(jobSchedule.getTask_id()).orElseThrow();
                if (history.getStatus().equals(Status.PENDING)) {

                    System.out.println("Lock acquired by " + EXECUTOR_ID);

                    history.setStatus(Status.EXECUTING);
                    jobExecutionHistoryRepository.save(history);


                    CompletableFuture<String> responseFuture = CompletableFuture.supplyAsync(() -> {
                        try {
                            String targetUrl = url; //+ "/" + jobSchedule.getTask_id();
                            System.out.println("Calling url ... " + targetUrl);
                            return restTemplate.postForObject(targetUrl, null, String.class);
                        } catch (HttpClientErrorException e) {
                            System.out.println("Error calling URL: " + e.getMessage());
                            return null;
                        }
                    });

                    responseFuture.thenAccept(response -> {
                        if (response != null) {
                            System.out.println("Response ... " + response);
                        }
                    });

                    // Update history with final status after processing response (optional)
                    responseFuture.thenAccept(response -> {
                        history.setStatus(response != null ? Status.DONE : Status.FAILED); // Update based on response
                        jobExecutionHistoryRepository.save(history);
                        System.out.println("set to be done by executor " + EXECUTOR_ID);
                    });


                }
            } else {
                System.out.println("Another instance is updating job " + jobSchedule.getTask_id() + ". Skipping.");
            }
        } catch (InterruptedException e) {
            System.out.println("There was an error getting lock: " + e);
        } finally {
            if (lock.isLocked() && lock.isHeldByCurrentThread()) {
                lock.unlock();
                System.out.println("unlocked by " + EXECUTOR_ID);
            }
        }
    }
}

