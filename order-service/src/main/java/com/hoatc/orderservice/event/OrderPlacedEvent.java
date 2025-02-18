package com.hoatc.orderservice.event;

import lombok.Getter;
import lombok.Setter;
import org.springframework.context.ApplicationEvent;

import java.util.List;


@Getter
@Setter
public class OrderPlacedEvent extends ApplicationEvent {
    private String orderNumber;
    private List<OrderItem> items;

    public OrderPlacedEvent(Object source, String orderNumber, List<OrderItem> items) {
        super(source);
        this.orderNumber = orderNumber;
        this.items = items;
    }

    public OrderPlacedEvent(String orderNumber, List<OrderItem> items) {
        super(orderNumber);
        this.orderNumber = orderNumber;
        this.items = items;
    }
}
