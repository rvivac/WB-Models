package com.wbscouting.api.config;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;

import static org.assertj.core.api.Assertions.assertThat;

class AsyncConfigTest {

    @Test
    @DisplayName("Deve configurar o ThreadPoolTaskExecutor com parâmetros corretos para envio de e-mails")
    void shouldConfigureEmailExecutorProperly() {
        AsyncConfig asyncConfig = new AsyncConfig();
        Executor executor = asyncConfig.emailExecutor();

        assertThat(executor).isInstanceOf(ThreadPoolTaskExecutor.class);

        ThreadPoolTaskExecutor taskExecutor = (ThreadPoolTaskExecutor) executor;
        assertThat(taskExecutor.getCorePoolSize()).isEqualTo(2);
        assertThat(taskExecutor.getMaxPoolSize()).isEqualTo(5);
        assertThat(taskExecutor.getQueueCapacity()).isEqualTo(50);
        assertThat(taskExecutor.getThreadNamePrefix()).isEqualTo("email-exec-");

        taskExecutor.destroy();
    }
}
