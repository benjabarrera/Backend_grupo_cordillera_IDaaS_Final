package com.grupocordillera.alertas.config;

import org.springframework.amqp.rabbit.annotation.EnableRabbit;
import org.springframework.amqp.rabbit.config.RetryInterceptorBuilder;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.config.SimpleRabbitListenerContainerFactory;
import org.springframework.amqp.rabbit.retry.RejectAndDontRequeueRecoverer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableRabbit
public class RabbitAuditoriaRetryConfig {

    @Bean
    public SimpleRabbitListenerContainerFactory auditoriaListenerFactory(
            ConnectionFactory connectionFactory) {

        SimpleRabbitListenerContainerFactory factory =
                new SimpleRabbitListenerContainerFactory();

        factory.setConnectionFactory(connectionFactory);
        factory.setConcurrentConsumers(1);
        factory.setDefaultRequeueRejected(false);

        factory.setAdviceChain(
                RetryInterceptorBuilder.stateless()
                        // Spring AMQP 4: dos reintentos tras el primer intento.
                        .maxRetries(2)
                        .backOffOptions(500L, 1.0, 1000L)
                        .recoverer(new RejectAndDontRequeueRecoverer())
                        .build()
        );

        return factory;
    }
}
