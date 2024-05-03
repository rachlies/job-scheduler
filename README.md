# Job Scheduler

## Running the system
### Prerequisites
- **Docker** installed.
- Maven installed in order to build the jar files for `Dockerfile`. (Not required if you are not making any code change.)

### Steps to run
- git clone the repo.
- go to `job-executor-service` directory. `cd job-scheduler/job-executor-service` and run `mvn clean package -DskipTests` (Not essential)
- go to `job-handler-service` directory. `cd job-scheduler/job-handler-service` && run `mvn clean package -DskipTests` (Not essential)
- go to projects root directory i.e `cd ../job-scheduler` if you in either of micro service directory.
- run `docker compose up --build`.
- wait for the services to spin up and run.
- make post and get requests using postman or any other clients. In case using postman client see below;
    - choose POST method type and add url `localhost:8080/timers` in the address bar
    - choose raw for body and hit send, eg request body below.
    - ```json 
    {
      "hours": 0,
      "seconds": 10,
      "minutes": 0,
      "url": "https://postman-echo.com/post < replace with your url>"
    }
    - you shall get a response that looks like
    - ```json
    {
        "time_left": 10,
        "id": 153
    }
    - Now you can use the `id` from the response to make GET call
    - choose GET method type and add url `localhost:8080/timers/10 <id in your response>` in the address bar, hit send
    - you shall get a response that looks like
    - ```json
    {
        "time_left": 4,
        "id": 153
    }


This project implements couple of endpoints for scheduling and fetching job details.

## About the Project
This project implements two endpoints for scheduling and fetching job details.

### Set Timers

- **Description**: Receives a JSON object containing hours, minutes, seconds, and a web URL. Starts an internal timer, which fires a webhook to the defined URL when the timer expires.
- **Method**: POST
- **URL**: `/timers`
- **Request Body**:
  ```json
  {
    "hours": 4,
    "minutes": 0,
    "seconds": 1,
    "url": "https://someserver.com"
  }
- **Response Body**:
  ```json
  { id: 1, time_left: 14401 }

### Get Timers

- **Description**: Receives the timer ID in the URL and returns the amount of seconds left until the timer expires.
- **Method**: GET
- **URL**: `/timers/1`
- **Response Body**:
  ```json
  { id: 1, time_left: 14401 }

### Additional Requirements
- Code Should handle invalid inputs; (Following are the requirements handled)
    - hours, minutes and seconds should be greater than or equals 0.
    - hours + minutes + seconds > 0
    - url should not be blank
    - for the get timers the id should be an existing id.
- The firing of the webhook must not be canceled by process restarts. Any timers that expired while the application was down should be triggered once the application comes back up.
- The solution should support horizontal scalability (running on multiple servers) to handle an increasing number of timers, including their creation and webhook firing.
- Each timer must be fired only once.

## Architecture
I have used micro service architecture in order to achieve the functionality in more robust way. Following are the components of my system.
- **job-handler-service**: This stateless SpringBoot web service is the entry point for the web requests i.e the aforementioned GET and POST endpoints. It schedules the timer (job) to be invoked (executed) once the set timer expires.
- **job-executor-service**: This stateless SpringBoot service is responsible for doing the execution of the job. In our case its making a call for the given timer (job).
- **postgres**: This is the data-store. I am using *postgres* in order to store a timer (job) related information. Both the statless services
  job-handler-service and job-executor-service are interacting with this db.
- **redis-distributed-lock**: Redis distributed lock is used in order to ensure that only one of the running job-executor-service instance is executing a job.

### Entities/Tables

#### Job Table

The `Job` table contains details of a job.

- **id (job_id)**: Unique identifier for the job.
- **hours**: Number of hours after which the job is scheduled to run.
- **minute**: Number of minutes after which the job is scheduled to run.
- **seconds**: Number of seconds after which the job is scheduled to run.
- **url**: Web URL to which the webhook will be sent when the timer expires.
- **created_at**: Timestamp indicating when the job was created.
- **updated_at**: Timestamp indicating when the job was last updated.

#### JobSchedules Table

The `JobSchedules` table contains details of a job's schedules.

- **id**: Unique identifier for the schedule.
- **execution_time** Its a Long value representing the UnixTime epoch seconds.
- **task_id**: Its the foreign key from the Job table.
- **created_at**: Timestamp indicating when the job was created.
- **updated_at**: Timestamp indicating when the job was last updated.

#### JobExecutionHistory Table

The `JobExecutionHistory` table contains status and execution history.
- **id**: Unique identifier for the JobExecutionHistory.
- **status** Its an enum {`PENDING`, `EXECUTING`, `DONE`, `FAILED`} representing the status of the job.
- **task_id**: Its the foreign key from the Job table.
- **created_at**: Timestamp indicating when the job was created.
- **updated_at**: Timestamp indicating when the job was last updated.

### System flows
#### POST Endpoint
- User sends post requests to `/timers`. This is handled by `job-handler-service`.
- An entry is created in Job table with the job's details.
- Another entry is created in JobSchedules table with execution time and job id. The execution time is calcualted in seconds granularity in order to execute the jobs which are scheduled to be executed even in 1 second.
- The JobExecutionHistory table gets a corresponding entry marking that the job is pending for exection. This is updated when the job-executor instance tries to execute the url. And consequently marked `done/failed`. This table is also consumed during system restart in order to ensure that all the expired pending jobs are executed as well.
#### GET ENDPOINT
- User sends get request to `timers/{id}`, handled by `job-handler-service`.
- The `job-handler-service` responds with the id and time left before expiry for this job.
#### ASYNCHRNOUS FLOW
- This is **most crucial** of the whole system. In this flow the the `job-execution-service` which runs every second in order to fetch the jobs scheduled for this second. And then sends a post request for the corresponding url. In order to achieve high-throughput I currently have spin up 2 instances of this service. And so there might be a chance of collision and both the instance will try to execute the same job. In order to avoid that I have used Redis's distributed lock. Once the job is executed the `JobExecutionHistory` is updated with the correct status.

### Assumptions
- Timers scheduled with 0 seconds to run are marked invalid for the system.
- Urls are validated for empty string only. Although the error handling is in place during the invokation of the post url.
- There is no headers (auth, type etc) configuration for the url execution.
- Only one replica of postgres db is kept. However this should be easy enought to configure multple replicas.

### Improvements
- **Choice of DB**: I have used postgres here and currently I din't index the tables. I believe indexing would be more beneficial in case our dataset grows. Ideall we need a database which actually efficiently fetches the jobs scheduled for the current epoch second. In my view the cassandra would be an ideal solution for production systems, as it scales well with huge writes and for better reads we can keep the execution_time as the partition key and task_id as sort key.

- **Distributed lock** I have used the distributed lock option to avoid duplicate invokation of urls. However this could also be achived by adding another column to JobSchedule table which marks the executor id and then each executor would only fetch their records. However This would require careful setup of config variables as our executor instances can scale up/down.

- **Frequency of executor service** The executor services is running every seconds in order to fetch the job scheduled for each seconds. This allows to precisely execute each task on time and make whole system strongly consistent. However there is downside to it, we are doing a lot of CPU and memory consumption. If we allow having a delay of upto 1 minutes then we can reduce the frequency by 60 times. And achive more throughput with this strategy.

- **Number of executor service instances** Currently 2 instances are deployed, however if our system is expecting hundreds or thousands of timers schedule requests per second then it would be ideal to scale up this `job-executor-service`.

- **Orchestration** - Currently I am using docker-compose to orhcestrate the services on dev enviroment, however if we want to productionize the system then we should resort to a better solution like kubernetes.



