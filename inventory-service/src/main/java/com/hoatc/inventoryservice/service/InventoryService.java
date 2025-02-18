package com.hoatc.inventoryservice.service;

import com.hoatc.inventoryservice.dto.InventoryItemDto;
import com.hoatc.inventoryservice.dto.InventoryRequest;
import com.hoatc.inventoryservice.dto.InventoryResponse;
import com.hoatc.inventoryservice.event.OrderItem;
import com.hoatc.inventoryservice.event.OrderPlacedEvent;
import com.hoatc.inventoryservice.model.Inventory;
import com.hoatc.inventoryservice.repository.InventoryRepository;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class InventoryService {

    private final InventoryRepository inventoryRepository;

    @Transactional(readOnly = true)
    @SneakyThrows
    public List<InventoryResponse> isInStock(InventoryRequest request) {
        log.info("Checking Inventory");
        List<InventoryItemDto> dtoList = request.getInventoryItemDtoList();
        List<String> skuCodes = dtoList.stream().map(InventoryItemDto::getSkuCode).collect(Collectors.toList());
        Map<String, Integer> inventoryMap = inventoryRepository.findBySkuCodeIn(skuCodes)
                .stream().collect(Collectors.toMap(Inventory::getSkuCode, Inventory::getQuantity));

        List<InventoryItemDto> dtoListInStock = dtoList.stream().filter(
                dto -> inventoryMap.get(dto.getSkuCode()) >= dto.getQuantity()
        ).toList();
        return dtoListInStock.stream()
                .map(item -> new InventoryResponse(item.getSkuCode(), item.getQuantity() > 0))
                .collect(Collectors.toList());

    }
    public List<Inventory> updateStock(OrderPlacedEvent event) {
        List<OrderItem> inventoryItemDtoList = event.getItems();
        return  inventoryItemDtoList.stream().map(request -> {
            Inventory inventory = inventoryRepository.findBySkuCode(request.getSkuCode())
                    .orElseThrow(() -> new RuntimeException("Product not found: " + request.getSkuCode()));
            inventory.setQuantity( inventory.getQuantity() - request.getQuantity());
            return inventoryRepository.save(inventory);
        }).collect(Collectors.toList());
    }
}
