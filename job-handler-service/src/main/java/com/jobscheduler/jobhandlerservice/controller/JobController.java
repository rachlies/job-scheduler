package com.jobscheduler.jobhandlerservice.controller;

import com.jobscheduler.jobhandlerservice.model.Dto.JobRequest;
import com.jobscheduler.jobhandlerservice.model.Dto.JobResponse;
import com.jobscheduler.jobhandlerservice.model.Job;
import com.jobscheduler.jobhandlerservice.service.JobService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/timers")
public class JobController {
    private final JobService jobService;

    @Autowired
    public JobController(JobService jobService) {
        this.jobService = jobService;
    }

    @GetMapping("/{id}")
    public JobResponse getJobById(@PathVariable Long id){
        return jobService.getJobById(id);
    }

    @PostMapping
    public JobResponse createJob(@RequestBody JobRequest jobRequest) {
        return jobService.createJob(new Job(
                jobRequest.hours(), jobRequest.minutes(), jobRequest.seconds(), jobRequest.url()));
    }
}
