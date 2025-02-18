package com.hoatc.orderservice.service;

import com.hoatc.orderservice.dto.*;
import com.hoatc.orderservice.event.OrderItem;
import com.hoatc.orderservice.event.OrderPlacedEvent;
import com.hoatc.orderservice.model.Order;
import com.hoatc.orderservice.model.OrderLineItems;
import com.hoatc.orderservice.repository.OrderRepository;
import io.micrometer.observation.Observation;
import io.micrometer.observation.ObservationRegistry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional
@Slf4j
public class OrderService {

    private final OrderRepository orderRepository;
    private final WebClient.Builder webClientBuilder;
    private final ObservationRegistry observationRegistry;
    private final ApplicationEventPublisher applicationEventPublisher;

    public String placeOrder(OrderRequest orderRequest) {
        Order order = new Order();
        order.setOrderNumber(UUID.randomUUID().toString());

        List<OrderLineItems> orderLineItems = orderRequest.getOrderLineItemsDtoList()
                .stream()
                .map(this::mapToDto)
                .toList();

        order.setOrderLineItemsList(orderLineItems);

        List<InventoryItemDto> dtoList = order.getOrderLineItemsList().stream()
                .map(item -> new InventoryItemDto(item.getSkuCode(), item.getQuantity()))
                .toList();
        InventoryRequest request = InventoryRequest.builder().inventoryItemDtoList(dtoList).build();
        // Call Inventory Service, and place order if product is in
        // stock
        Observation inventoryServiceObservation = Observation.createNotStarted("inventory-service-lookup",
                this.observationRegistry);
        inventoryServiceObservation.lowCardinalityKeyValue("call", "inventory-service");
        return inventoryServiceObservation.observe(() -> {
            InventoryResponse[] inventoryResponseArray = webClientBuilder.build().post()
                    .uri("http://inventory-service/api/inventory")
                    .bodyValue(request)
                    .retrieve()
                    .bodyToMono(InventoryResponse[].class)
                    .block();

            boolean allProductsInStock = orderLineItems.size() == inventoryResponseArray.length;

            if (allProductsInStock) {
                orderRepository.save(order);
                // publish Order Placed Event
                List<OrderItem> orderItems = orderLineItems.stream().map(item -> this.mapItemDto(item)).toList();;
                applicationEventPublisher.publishEvent(new OrderPlacedEvent(this, order.getOrderNumber(), orderItems));
                return "Order Placed";
            } else {
                throw new IllegalArgumentException("Product is not in stock, please try again later");
            }
        });

    }

    private OrderItem mapItemDto(OrderLineItems item) {
        OrderItem orderItem = new OrderItem();
        orderItem.setSkuCode(item.getSkuCode());
        orderItem.setQuantity(item.getQuantity());
        return orderItem;
    }

    private OrderLineItems mapToDto(OrderLineItemsDto orderLineItemsDto) {
        OrderLineItems orderLineItems = new OrderLineItems();
        orderLineItems.setPrice(orderLineItemsDto.getPrice());
        orderLineItems.setQuantity(orderLineItemsDto.getQuantity());
        orderLineItems.setSkuCode(orderLineItemsDto.getSkuCode());
        return orderLineItems;
    }
}
