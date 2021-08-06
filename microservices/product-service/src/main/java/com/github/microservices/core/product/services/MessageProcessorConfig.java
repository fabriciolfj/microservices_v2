package com.github.microservices.core.product.services;

import com.github.api.core.event.Event;
import com.github.api.core.product.Product;
import com.github.api.core.product.ProductService;
import com.github.api.exceptions.EventProcessingException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.function.Consumer;

@Configuration
public class MessageProcessorConfig {

    private final ProductService productService;
    private static final Logger LOG = LoggerFactory.getLogger(MessageProcessorConfig.class);

    @Autowired
    public MessageProcessorConfig(final ProductService productService) {
        this.productService = productService;
    }

    @Bean
    public Consumer<Event<Integer, Product>> messageProcessor() {
        return event -> {
            LOG.info("Consumer message: {}", event.getData().toString());
            switch (event.getEventType()) {
                case CREATE:
                    productService.createProduct(event.getData()).block();
                    break;
                case DELETE:
                    productService.deleteProduct(event.getKey()).block();
                    break;
                default:
                    throw new EventProcessingException("Incorrect event type: " + event.getEventType());
            }
        };
    }
}
