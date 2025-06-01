package personal.social.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.task.TaskExecutor;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;

/**
 * Async Configuration cho WebSocket operations
 * Hỗ trợ async processing cho message handling và broadcasting
 */
@Configuration
@EnableAsync
@EnableScheduling
@Slf4j
public class AsyncConfig {
    /**
     * Thread pool cho WebSocket message processing
     */
    @Bean("websocketTaskExecutor")
    public Executor websocketTaskExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(5);
        executor.setMaxPoolSize(20);
        executor.setQueueCapacity(100);
        executor.setThreadNamePrefix("WebSocket-");
        executor.setKeepAliveSeconds(60);
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setAwaitTerminationSeconds(30);

        // Exception handler
        executor.setRejectedExecutionHandler((r, executor1) -> {
            log.warn("WebSocket task rejected: {}", r.toString());
            // Có thể implement fallback logic ở đây
        });

        executor.initialize();
        return executor;
    }
    
    /**
     * Thread pool cho general async operations
     */
    @Bean("generalTaskExecutor")
    public Executor generalTaskExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(3);
        executor.setMaxPoolSize(10);
        executor.setQueueCapacity(50);
        executor.setThreadNamePrefix("General-");
        executor.setKeepAliveSeconds(60);
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setAwaitTerminationSeconds(30);

        executor.setRejectedExecutionHandler((r, executor1) -> {
            log.warn("General task rejected: {}", r.toString());
        });

        executor.initialize();
        return executor;
    }
}
