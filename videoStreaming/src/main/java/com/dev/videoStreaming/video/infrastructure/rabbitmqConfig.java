package com.dev.videoStreaming.video.infrastructure;

import org.springframework.amqp.core.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.amqp.rabbit.connection.CachingConnectionFactory;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;


@Configuration
public class rabbitmqConfig {

    @Value("${spring.rabbitmq.host:localhost}")
    private String host;

    @Value("${spring.rabbitmq.port:5672}")
    private int port;

    @Value("${spring.rabbitmq.username:guest}")
    private String username;

    @Value("${spring.rabbitmq.password:guest}")
    private String password;

    @Value("${spring.rabbitmq.virtual-host:/}")
    private String virtualHost;

    @Bean
    public ConnectionFactory connectionFactory() {
        CachingConnectionFactory factory = new CachingConnectionFactory(host, port);
        factory.setUsername(username);
        factory.setPassword(password);
        factory.setVirtualHost(virtualHost);
        return factory;
    }

    @Bean
    public TopicExchange videoExchange() {
        return new TopicExchange("video-exchange");
    }

    @Bean
    public Queue videoUploadQueue() {
        return QueueBuilder.durable("video-upload").build();
    }

    @Bean
    public Queue videoReadyQueue() {
        return QueueBuilder.durable("video-ready").build();
    }

    @Bean
    public Binding videoUploadBinding(Queue videoUploadQueue, TopicExchange videoExchange) {
        return BindingBuilder.bind(videoUploadQueue).to(videoExchange).with("video-upload");
    }

    @Bean
    public Binding videoReadyBinding(Queue videoReadyQueue, TopicExchange videoExchange) {
        return BindingBuilder.bind(videoReadyQueue).to(videoExchange).with("video-ready");
    }

    @Bean
    public org.springframework.amqp.support.converter.MessageConverter jsonMessageConverter() {
        return new org.springframework.amqp.support.converter.JacksonJsonMessageConverter();
    }

    @Bean
    public AmqpTemplate amqpTemplate(ConnectionFactory connectionFactory, org.springframework.amqp.support.converter.MessageConverter messageConverter) {
        RabbitTemplate rabbitTemplate = new RabbitTemplate(connectionFactory);
        rabbitTemplate.setMessageConverter(messageConverter);
        return rabbitTemplate;
    }
}
