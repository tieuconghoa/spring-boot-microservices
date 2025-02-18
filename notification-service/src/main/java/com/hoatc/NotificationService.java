package com.hoatc;

import io.micrometer.observation.Observation;
import io.micrometer.observation.ObservationRegistry;
import io.micrometer.tracing.Tracer;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.logging.log4j.util.Strings;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

@Slf4j
@Service
@AllArgsConstructor
public class NotificationService {

    private final ObservationRegistry observationRegistry;
    private final Tracer tracer;
    private final WebClient.Builder webClientBuilder;

    @KafkaListener(topics = "notificationTopic")
    public String handleNotification(OrderPlacedEvent orderPlacedEvent) {
        Observation.createNotStarted("on-message", this.observationRegistry).observe(() -> {
            log.info("Got message <{}>", orderPlacedEvent);
            log.info("TraceId- {}, Received Notification for Order - {}", this.tracer.currentSpan().context().traceId(),
                    orderPlacedEvent.getOrderNumber());
        });
        // send out an email notification
        Observation inventoryServiceObservation = Observation.createNotStarted("inventory-service-lookup2",
                this.observationRegistry);
        inventoryServiceObservation.lowCardinalityKeyValue("call", "inventory-service");
        return inventoryServiceObservation.observe(() -> {
            String str = webClientBuilder.build().put()
                    .uri("http://inventory-service/api/inventory")
                    .bodyValue(orderPlacedEvent)
                    .retrieve()
                    .bodyToMono(String.class)
                    .block();
            if (!Strings.isEmpty(str)) {
                return "Order Placed";
            } else {
                throw new IllegalArgumentException("Product is not in stock, please try again later");
            }
        });
    }
}
