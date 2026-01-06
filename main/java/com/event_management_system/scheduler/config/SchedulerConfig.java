package com.event_management_system.scheduler.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;


@Configuration
@EnableScheduling  
public class SchedulerConfig {

    
    @Bean
    public ThreadPoolTaskScheduler taskScheduler() {
        ThreadPoolTaskScheduler scheduler = new ThreadPoolTaskScheduler();
        
        scheduler.setPoolSize(5);
        
        scheduler.setThreadNamePrefix("scheduler-");
        
        scheduler.setAwaitTerminationSeconds(60);
        
        scheduler.setWaitForTasksToCompleteOnShutdown(true);
        
        scheduler.initialize();
        
        return scheduler;
    }
}
