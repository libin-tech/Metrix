package com.bintech.metrix.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.AsyncConfigurer;
import org.springframework.scheduling.annotation.EnableAsync;

import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * 异步与AOP配置，使用JDK21虚拟线程执行异步任务。
 */
@Configuration
@EnableAsync
public class AsyncConfig implements AsyncConfigurer {

    @Override
    @Bean(name = "taskExecutor")
    public Executor getAsyncExecutor() {
        return Executors.newVirtualThreadPerTaskExecutor();
    }

    /**
     * 邮件投递专用执行器，与其他异步任务隔离，避免 SMTP 网络抖动占用通用任务资源。
     */
    @Bean(name = "mailTaskExecutor", destroyMethod = "close")
    public ExecutorService mailTaskExecutor() {
        return Executors.newThreadPerTaskExecutor(Thread.ofVirtual().name("metrix-mail-", 0).factory());
    }
}
