package com.gomoku.sync.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;

@Configuration
public class BotSchedulerConfig {

    @Bean(destroyMethod = "shutdown")
    public ScheduledExecutorService gomokuBotScheduler() {
        return Executors.newScheduledThreadPool(
                2,
                r -> {
                    Thread t = new Thread(r, "gomoku-bot-scheduler");
                    t.setDaemon(true);
                    return t;
                });
    }
}
