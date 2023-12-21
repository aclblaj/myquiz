package com.unitbv.myquiz.tasks;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;

/**
 * Configuration class for the thread pool used to read and parse the input files.
 * String Boot manages the custom thread pool. For each file we start a new thread.
 * The thread pool is used to limit the number of files processed in parallel.
 * Using CompletableFuture.supplyAsync() we can start a new thread for each method call.
*/
@Configuration
public class ThreadPoolConfig {
    @Bean(name = "readAndParseFileTaskExecutor")
    public Executor readAndParseFileThreadPoolTaskExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(10);
        executor.setMaxPoolSize(25);
        executor.setThreadNamePrefix("readAndParseFileTaskExecutor-");
        executor.initialize();
        return executor;
    }
}
