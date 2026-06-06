package br.com.uebiescola.core.infrastructure.messaging;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitMQConfig {

    public static final String SCHOOL_DELETED_EXCHANGE = "ex.school.deleted";
    public static final String SCHOOL_DELETED_KEY = "school.deleted";

    // Consumer: plan.payment.confirmed — disparado pelo plans quando escola
    // paga fatura SaaS. Usado pra creditar indicacao (#66).
    public static final String PLAN_PAYMENT_CONFIRMED_EXCHANGE = "ex.plan.payment.confirmed";
    public static final String PLAN_PAYMENT_CONFIRMED_KEY = "plan.payment.confirmed";
    public static final String PLAN_PAYMENT_CONFIRMED_QUEUE = "q.plan.payment.confirmed.core";

    public static final String PLAN_PAYMENT_CONFIRMED_DLX = "ex.plan.payment.confirmed.dlx";
    public static final String PLAN_PAYMENT_CONFIRMED_DLQ = "q.plan.payment.confirmed.core.dlq";
    public static final String PLAN_PAYMENT_CONFIRMED_DLQ_KEY = "core";

    @Bean
    public DirectExchange schoolDeletedExchange() {
        return new DirectExchange(SCHOOL_DELETED_EXCHANGE, true, false);
    }

    @Bean
    public DirectExchange planPaymentConfirmedExchange() {
        return new DirectExchange(PLAN_PAYMENT_CONFIRMED_EXCHANGE, true, false);
    }

    @Bean
    public Queue planPaymentConfirmedQueue() {
        return QueueBuilder.durable(PLAN_PAYMENT_CONFIRMED_QUEUE)
                .withArgument("x-dead-letter-exchange", PLAN_PAYMENT_CONFIRMED_DLX)
                .withArgument("x-dead-letter-routing-key", PLAN_PAYMENT_CONFIRMED_DLQ_KEY)
                .build();
    }

    @Bean
    public Binding planPaymentConfirmedBinding(Queue planPaymentConfirmedQueue,
                                               DirectExchange planPaymentConfirmedExchange) {
        return BindingBuilder.bind(planPaymentConfirmedQueue)
                .to(planPaymentConfirmedExchange)
                .with(PLAN_PAYMENT_CONFIRMED_KEY);
    }

    @Bean
    public DirectExchange planPaymentConfirmedDlx() {
        return new DirectExchange(PLAN_PAYMENT_CONFIRMED_DLX, true, false);
    }

    @Bean
    public Queue planPaymentConfirmedDlq() {
        return QueueBuilder.durable(PLAN_PAYMENT_CONFIRMED_DLQ).build();
    }

    @Bean
    public Binding planPaymentConfirmedDlqBinding(Queue planPaymentConfirmedDlq,
                                                  DirectExchange planPaymentConfirmedDlx) {
        return BindingBuilder.bind(planPaymentConfirmedDlq)
                .to(planPaymentConfirmedDlx)
                .with(PLAN_PAYMENT_CONFIRMED_DLQ_KEY);
    }

    @Bean
    public Jackson2JsonMessageConverter messageConverter() {
        return new Jackson2JsonMessageConverter();
    }

    @Bean
    public RabbitTemplate rabbitTemplate(ConnectionFactory connectionFactory,
                                         Jackson2JsonMessageConverter converter) {
        RabbitTemplate template = new RabbitTemplate(connectionFactory);
        template.setMessageConverter(converter);
        return template;
    }
}
