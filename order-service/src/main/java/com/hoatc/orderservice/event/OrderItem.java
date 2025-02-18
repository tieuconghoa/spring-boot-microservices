package com.hoatc.orderservice.event;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class OrderItem {

    private String skuCode;
    private int quantity;
}
