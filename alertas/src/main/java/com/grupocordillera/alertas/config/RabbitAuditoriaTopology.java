package com.grupocordillera.alertas.config;

import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.Declarables;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitAuditoriaTopology {
    public static final String MAIN_EXCHANGE = "cordillera.eventos";
    public static final String MAIN_QUEUE = "cordillera.auditoria";
    public static final String MAIN_ROUTING = "ventas.audit";
    public static final String DLX = "cordillera.eventos.dlx";
    public static final String DLQ = "cordillera.auditoria.dlq";
    public static final String DLQ_ROUTING = "ventas.audit.error";

    @Bean
    public Declarables auditoriaDeclarations() {
        DirectExchange exchange = new DirectExchange(MAIN_EXCHANGE, true, false);
        DirectExchange deadExchange = new DirectExchange(DLX, true, false);
        Queue principal = QueueBuilder.durable(MAIN_QUEUE)
            .deadLetterExchange(DLX)
            .deadLetterRoutingKey(DLQ_ROUTING)
            .build();
        Queue deadQueue = QueueBuilder.durable(DLQ).build();
        return new Declarables(exchange, deadExchange, principal, deadQueue,
            BindingBuilder.bind(principal).to(exchange).with(MAIN_ROUTING),
            BindingBuilder.bind(deadQueue).to(deadExchange).with(DLQ_ROUTING));
    }
}
