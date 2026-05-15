package com.unitbv.myquiz.app.tasks;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "myquiz.tasks")
public class ThreadPoolTaskProperties {
    private PoolProperties readAndParse = new PoolProperties(10, 25, 0);
    private PoolProperties duplicateCheck = new PoolProperties(20, 20, 200);

    public PoolProperties getReadAndParse() {
        return readAndParse;
    }

    public void setReadAndParse(PoolProperties readAndParse) {
        this.readAndParse = readAndParse;
    }

    public PoolProperties getDuplicateCheck() {
        return duplicateCheck;
    }

    public void setDuplicateCheck(PoolProperties duplicateCheck) {
        this.duplicateCheck = duplicateCheck;
    }

    public static class PoolProperties {
        private int corePoolSize;
        private int maxPoolSize;
        private int queueCapacity;
        private boolean waitForTasksToCompleteOnShutdown = true;
        private int awaitTerminationSeconds = 120;

        public PoolProperties() {
        }

        public PoolProperties(int corePoolSize, int maxPoolSize, int queueCapacity) {
            this.corePoolSize = corePoolSize;
            this.maxPoolSize = maxPoolSize;
            this.queueCapacity = queueCapacity;
        }

        public int getCorePoolSize() {
            return corePoolSize;
        }

        public void setCorePoolSize(int corePoolSize) {
            this.corePoolSize = corePoolSize;
        }

        public int getMaxPoolSize() {
            return maxPoolSize;
        }

        public void setMaxPoolSize(int maxPoolSize) {
            this.maxPoolSize = maxPoolSize;
        }

        public int getQueueCapacity() {
            return queueCapacity;
        }

        public void setQueueCapacity(int queueCapacity) {
            this.queueCapacity = queueCapacity;
        }

        public boolean isWaitForTasksToCompleteOnShutdown() {
            return waitForTasksToCompleteOnShutdown;
        }

        public void setWaitForTasksToCompleteOnShutdown(boolean waitForTasksToCompleteOnShutdown) {
            this.waitForTasksToCompleteOnShutdown = waitForTasksToCompleteOnShutdown;
        }

        public int getAwaitTerminationSeconds() {
            return awaitTerminationSeconds;
        }

        public void setAwaitTerminationSeconds(int awaitTerminationSeconds) {
            this.awaitTerminationSeconds = awaitTerminationSeconds;
        }
    }
}
