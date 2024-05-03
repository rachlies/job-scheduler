package com.jobscheduler.jobexecutorservice;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class JobExecutorServiceApplication {

	public static void main(String[] args) {
		SpringApplication.run(JobExecutorServiceApplication.class, args);
	}

}
