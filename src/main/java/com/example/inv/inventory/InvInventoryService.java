package com.example.inv.inventory;

import org.springframework.stereotype.Service;
import java.util.List;
import java.util.Optional;

@Service
public class InvInventoryService {

    private final InvInventoryRepository inventoryRepository;

    public InvInventoryService(InvInventoryRepository inventoryRepository) {
        this.inventoryRepository = inventoryRepository;
    }

    public List<InventoryItem> findAll() {
        return inventoryRepository.findAll();
    }

    public Optional<InventoryItem> findById(Long id) {
        return inventoryRepository.findById(id);
    }

    public InventoryItem adjustStock(Long itemId, int delta) {
        InventoryItem item = inventoryRepository.findById(itemId)
            .orElseThrow(() -> new RuntimeException("Item not found: " + itemId));
        item.setQuantity(item.getQuantity() + delta);
        return inventoryRepository.save(item);
    }

    public List<InventoryItem> findLowStock(int threshold) {
        return inventoryRepository.findByQuantityLessThan(threshold);
    }
}
// modified
