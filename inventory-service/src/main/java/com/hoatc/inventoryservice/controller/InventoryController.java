package com.hoatc.inventoryservice.controller;

import com.hoatc.inventoryservice.dto.InventoryRequest;
import com.hoatc.inventoryservice.dto.InventoryResponse;
import com.hoatc.inventoryservice.event.OrderPlacedEvent;
import com.hoatc.inventoryservice.model.Inventory;
import com.hoatc.inventoryservice.service.InventoryService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/inventory")
@RequiredArgsConstructor
@Slf4j
public class InventoryController {

    private final InventoryService inventoryService;

    // http://localhost:8082/api/inventory?skuCode=iphone-13&skuCode=iphone13-red
    @PostMapping
    @ResponseStatus(HttpStatus.OK)
    public List<InventoryResponse> isInStock(@RequestBody InventoryRequest inventoryRequest) {
        log.info("Received inventory check request for skuCode: {}", inventoryRequest);
        return inventoryService.isInStock(inventoryRequest);
    }

    @PutMapping
    @ResponseStatus(HttpStatus.ACCEPTED)
    public String updateStock(@RequestBody OrderPlacedEvent event) {
        List<Inventory> inventoryList = inventoryService.updateStock(event);
        if (inventoryList.size() > 0) {
            return "Successfully updated stock";
        } else {
            throw new RuntimeException("Failed to update stock");
        }
    }
}

