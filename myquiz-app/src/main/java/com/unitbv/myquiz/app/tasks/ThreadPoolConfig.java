package com.unitbv.myquiz.app.tasks;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;
import java.util.concurrent.ThreadPoolExecutor;

/**
 * Configuration class for the thread pool used to read and parse the input files.
 * String Boot manages the custom thread pool. For each file we start a new thread.
 * The thread pool is used to limit the number of files processed in parallel.
 * Using CompletableFuture.supplyAsync() we can start a new thread for each method call.
*/
@Configuration
@EnableConfigurationProperties(ThreadPoolTaskProperties.class)
public class ThreadPoolConfig {
    private final ThreadPoolTaskProperties properties;

    public ThreadPoolConfig(ThreadPoolTaskProperties properties) {
        this.properties = properties;
    }

    @Bean(name = "readAndParseFileTaskExecutor")
    public Executor readAndParseFileThreadPoolTaskExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(properties.getReadAndParse().getCorePoolSize());
        executor.setMaxPoolSize(properties.getReadAndParse().getMaxPoolSize());
        executor.setThreadNamePrefix("readAndParseFileTaskExecutor-");
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());
        executor.setWaitForTasksToCompleteOnShutdown(properties.getReadAndParse().isWaitForTasksToCompleteOnShutdown());
        executor.setAwaitTerminationSeconds(properties.getReadAndParse().getAwaitTerminationSeconds());
        executor.initialize();
        return executor;
    }

    @Bean(name = "duplicateQuestionCheckTaskExecutor")
    public Executor duplicateQuestionCheckTaskExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(properties.getDuplicateCheck().getCorePoolSize());
        executor.setMaxPoolSize(properties.getDuplicateCheck().getMaxPoolSize());
        executor.setQueueCapacity(properties.getDuplicateCheck().getQueueCapacity());
        executor.setThreadNamePrefix("duplicateQuestionCheckTaskExecutor-");
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());
        executor.setWaitForTasksToCompleteOnShutdown(properties.getDuplicateCheck().isWaitForTasksToCompleteOnShutdown());
        executor.setAwaitTerminationSeconds(properties.getDuplicateCheck().getAwaitTerminationSeconds());
        executor.initialize();
        return executor;
    }
}
