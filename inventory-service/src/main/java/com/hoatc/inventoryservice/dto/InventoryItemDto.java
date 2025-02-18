package com.hoatc.inventoryservice.dto;

import lombok.*;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
public class InventoryItemDto {

    private String skuCode;

    private int quantity;

}
