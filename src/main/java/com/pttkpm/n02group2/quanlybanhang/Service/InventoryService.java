package com.pttkpm.n02group2.quanlybanhang.Service;

import com.pttkpm.n02group2.quanlybanhang.Model.Inventory;
import com.pttkpm.n02group2.quanlybanhang.Repository.InventoryRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
public class InventoryService {

    @Autowired
    private InventoryRepository inventoryRepository;

    // Lấy tất cả inventory - PHƯƠNG THỨC BỊ THIẾU
    public List<Inventory> getAllInventories() {
        return inventoryRepository.findAll();
    }

    // Alias cho getAllInventories
    public List<Inventory> getAllInventoryItems() {
        return getAllInventories();
    }

    // Lấy inventory theo ID
    public Inventory getInventoryItemById(Long id) {
        return inventoryRepository.findById(id).orElse(null);
    }

    // Thêm inventory item
    public Inventory addInventoryItem(Inventory inventory) {
        return inventoryRepository.save(inventory);
    }

    // Cập nhật inventory item
    public Inventory updateInventoryItem(Long id, Inventory inventory) {
        inventory.setId(id);
        return inventoryRepository.save(inventory);
    }

    // Xóa inventory item
    public void deleteInventoryItem(Long id) {
        inventoryRepository.deleteById(id);
    }

    // Tạo inventory mới
    public Inventory createInventory(Inventory inventory) {
        return inventoryRepository.save(inventory);
    }

    // Lấy inventory theo ID (Optional)
    public Optional<Inventory> getInventoryById(Long id) {
        return inventoryRepository.findById(id);
    }

    // Cập nhật inventory (Optional)
    public Optional<Inventory> updateInventory(Long id, Inventory inventoryDetails) {
        Optional<Inventory> optionalInventory = getInventoryById(id);
        if (optionalInventory.isPresent()) {
            Inventory inventory = optionalInventory.get();
            
            inventory.setProductName(inventoryDetails.getProductName());
            inventory.setQuantity(inventoryDetails.getQuantity());
            inventory.setUnitPrice(inventoryDetails.getUnitPrice());
            inventory.setDescription(inventoryDetails.getDescription());
            inventory.setCategory(inventoryDetails.getCategory());
            
            return Optional.of(inventoryRepository.save(inventory));
        }
        return Optional.empty();
    }

    // Xóa inventory (boolean)
    public boolean deleteInventory(Long id) {
        if (inventoryRepository.existsById(id)) {
            inventoryRepository.deleteById(id);
            return true;
        }
        return false;
    }

    // Tìm inventory theo tên sản phẩm
    public List<Inventory> getInventoriesByProductName(String productName) {
        return inventoryRepository.findByProductNameContainingIgnoreCase(productName);
    }

    // Tìm inventory theo danh mục
    public List<Inventory> getInventoriesByCategory(String category) {
        return inventoryRepository.findByCategory(category);
    }

    // Tìm inventory có số lượng thấp
    public List<Inventory> getLowStockInventories(Integer threshold) {
        return inventoryRepository.findByQuantityLessThan(threshold);
    }

    // Cập nhật số lượng tồn kho
    @Transactional
    public void updateInventory(Long inventoryId, Integer quantityChange) {
        Optional<Inventory> inventoryOpt = inventoryRepository.findById(inventoryId);
        if (!inventoryOpt.isPresent()) {
            throw new RuntimeException("Inventory not found with ID: " + inventoryId);
        }

        Inventory inventory = inventoryOpt.get();
        int newQuantity = inventory.getQuantity() + quantityChange;
        if (newQuantity < 0) {
            throw new RuntimeException("Insufficient inventory with ID: " + inventoryId);
        }

        inventory.setQuantity(newQuantity);
        inventoryRepository.save(inventory);
    }

    // Lấy thông tin tồn kho theo ID sản phẩm
    public Inventory getInventory(Long productId) {
        return inventoryRepository.findByProduct_Id(productId)
            .orElseThrow(() -> new RuntimeException("Inventory not found for product ID: " + productId));
    }
}