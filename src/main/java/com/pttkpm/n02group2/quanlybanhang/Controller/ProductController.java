package com.pttkpm.n02group2.quanlybanhang.Controller;

import com.pttkpm.n02group2.quanlybanhang.Model.Product;
import com.pttkpm.n02group2.quanlybanhang.Service.ProductService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import jakarta.servlet.http.HttpSession;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Controller
public class ProductController {

    @Autowired
    private ProductService productService;

    // ==================== ADMIN ENDPOINTS ====================

    // ==================== ADMIN ENDPOINTS ====================

// Danh sách sản phẩm + tìm kiếm + phân trang
@GetMapping("/admin/products")
public String adminListProducts(@RequestParam(defaultValue = "0") int page,
                              @RequestParam(defaultValue = "10") int size,
                              @RequestParam(required = false) String search,
                              @RequestParam(required = false) String category,
                              @RequestParam(required = false) String stockStatus,
                              @RequestParam(required = false) Double minPrice,
                              @RequestParam(required = false) Double maxPrice,
                              @RequestParam(defaultValue = "id") String sortBy,
                              @RequestParam(defaultValue = "desc") String sortDir,
                              Model model) {
    try {
        // Tìm kiếm nâng cao với các điều kiện
        Page<Product> productPage = productService.searchProducts(
            search, category, stockStatus, minPrice, maxPrice, page, size);

        // Thống kê tổng quan
        List<Product> allProducts = productService.getAllProducts();
        long totalProducts = allProducts != null ? allProducts.size() : 0;
        long inStockCount = allProducts != null ? allProducts.stream().filter(p -> p.getQuantity() > 10).count() : 0;
        long lowStockCount = allProducts != null ? allProducts.stream().filter(p -> p.getQuantity() > 0 && p.getQuantity() <= 10).count() : 0;
        long outOfStockCount = allProducts != null ? allProducts.stream().filter(p -> p.getQuantity() == 0).count() : 0;

        model.addAttribute("products", productPage != null ? productPage.getContent() : new ArrayList<>());
        model.addAttribute("currentPage", page);
        model.addAttribute("totalPages", productPage != null ? productPage.getTotalPages() : 0);
        model.addAttribute("totalElements", productPage != null ? productPage.getTotalElements() : 0);
        model.addAttribute("totalProducts", totalProducts);
        model.addAttribute("inStockCount", inStockCount);
        model.addAttribute("lowStockCount", lowStockCount);
        model.addAttribute("outOfStockCount", outOfStockCount);

        // Add search params for form
        model.addAttribute("search", search);
        model.addAttribute("category", category);
        model.addAttribute("stockStatus", stockStatus);
        model.addAttribute("minPrice", minPrice);
        model.addAttribute("maxPrice", maxPrice);
        model.addAttribute("sortBy", sortBy);
        model.addAttribute("sortDir", sortDir);

        // Thêm empty product object cho form thêm mới
        model.addAttribute("newProduct", new Product());

    } catch (Exception e) {
        model.addAttribute("error", "Có lỗi xảy ra khi tải dữ liệu: " + e.getMessage());
    }

    return "admin/products/index";
}

    // Form thêm sản phẩm (endpoint tương thích)
    @GetMapping("/admin/products/create")
    public String showCreateProductForm(Model model) {
        model.addAttribute("product", new Product());
        return "admin/products/add";
    }

    // Form thêm sản phẩm
    @GetMapping("/admin/products/add")
    public String showAddProductForm(Model model) {
        model.addAttribute("product", new Product());
        return "admin/products/add";
    }

    // Xử lý thêm sản phẩm mới
    @PostMapping("/admin/products/add")
    public String addProduct(@ModelAttribute Product product,
                           @RequestParam(value = "imageFile", required = false) MultipartFile imageFile,
                           RedirectAttributes redirectAttributes) {
        try {
            // Validation
            if (product.getName() == null || product.getName().trim().isEmpty()) {
                redirectAttributes.addFlashAttribute("error", "Tên sản phẩm không được để trống!");
                return "redirect:/admin/products";
            }
            
            if (product.getPrice() == null || product.getPrice() <= 0) {
                redirectAttributes.addFlashAttribute("error", "Giá sản phẩm phải lớn hơn 0!");
                return "redirect:/admin/products";
            }

            if (product.getQuantity() == null || product.getQuantity() < 0) {
                redirectAttributes.addFlashAttribute("error", "Số lượng không được âm!");
                return "redirect:/admin/products";
            }

            // Xử lý upload ảnh
            if (imageFile != null && !imageFile.isEmpty()) {
                try {
                    String imagePath = saveImage(imageFile);
                    product.setImagePath(imagePath);
                } catch (IOException e) {
                    redirectAttributes.addFlashAttribute("error", "Lỗi khi upload ảnh: " + e.getMessage());
                    return "redirect:/admin/products";
                }
            }

            Product newProduct = productService.createProduct(product);
            redirectAttributes.addFlashAttribute("success", "Đã thêm sản phẩm mới thành công!");

        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Có lỗi xảy ra khi thêm sản phẩm: " + e.getMessage());
        }

        return "redirect:/admin/products";
    }

    // Form chỉnh sửa sản phẩm
    @GetMapping("/admin/products/edit/{id}")
    public String showEditProductForm(@PathVariable Long id, Model model) {
        try {
            Optional<Product> product = productService.getProductById(id);
            if (product.isPresent()) {
                model.addAttribute("product", product.get());
                return "admin/products/edit";
            } else {
                model.addAttribute("error", "Không tìm thấy sản phẩm!");
            }
        } catch (Exception e) {
            model.addAttribute("error", "Có lỗi xảy ra: " + e.getMessage());
        }
        return "redirect:/admin/products";
    }

    // Xử lý cập nhật sản phẩm
    @PostMapping("/admin/products/update")
    public String updateProduct(@ModelAttribute Product product,
                              @RequestParam(value = "imageFile", required = false) MultipartFile imageFile,
                              RedirectAttributes redirectAttributes) {
        try {
            if (product.getId() == null) {
                redirectAttributes.addFlashAttribute("error", "Thiếu ID sản phẩm!");
                return "redirect:/admin/products";
            }

            // Validation
            if (product.getName() == null || product.getName().trim().isEmpty()) {
                redirectAttributes.addFlashAttribute("error", "Tên sản phẩm không được để trống!");
                return "redirect:/admin/products/edit/" + product.getId();
            }
            
            if (product.getPrice() == null || product.getPrice() <= 0) {
                redirectAttributes.addFlashAttribute("error", "Giá sản phẩm phải lớn hơn 0!");
                return "redirect:/admin/products/edit/" + product.getId();
            }

            if (product.getQuantity() == null || product.getQuantity() < 0) {
                redirectAttributes.addFlashAttribute("error", "Số lượng không được âm!");
                return "redirect:/admin/products/edit/" + product.getId();
            }

            // Xử lý upload ảnh mới
            if (imageFile != null && !imageFile.isEmpty()) {
                try {
                    String imagePath = saveImage(imageFile);
                    product.setImagePath(imagePath);
                } catch (IOException e) {
                    redirectAttributes.addFlashAttribute("error", "Lỗi khi upload ảnh: " + e.getMessage());
                    return "redirect:/admin/products/edit/" + product.getId();
                }
            }

            Product updatedProduct = productService.updateProduct(product.getId(), product);
            redirectAttributes.addFlashAttribute("success", "Đã cập nhật sản phẩm thành công!");

        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Có lỗi xảy ra khi cập nhật sản phẩm: " + e.getMessage());
        }

        return "redirect:/admin/products";
    }

    // Xem chi tiết sản phẩm
    @GetMapping("/admin/products/view/{id}")
    public String viewProduct(@PathVariable Long id, Model model) {
        try {
            Optional<Product> product = productService.getProductById(id);
            if (product.isPresent()) {
                model.addAttribute("product", product.get());
                return "admin/products/view";
            } else {
                model.addAttribute("error", "Không tìm thấy sản phẩm!");
            }
        } catch (Exception e) {
            model.addAttribute("error", "Có lỗi xảy ra: " + e.getMessage());
        }
        return "redirect:/admin/products";
    }

    // Xóa sản phẩm
    @PostMapping("/admin/products/delete")
    public String deleteProduct(@RequestParam Long id,
                              RedirectAttributes redirectAttributes) {
        try {
            Optional<Product> product = productService.getProductById(id);
            if (product.isPresent()) {
                productService.deleteProduct(id);
                redirectAttributes.addFlashAttribute("success", "Đã xóa sản phẩm thành công!");
            } else {
                redirectAttributes.addFlashAttribute("error", "Không tìm thấy sản phẩm!");
            }
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Có lỗi xảy ra khi xóa sản phẩm: " + e.getMessage());
        }
        return "redirect:/admin/products";
    }
@GetMapping("/admin/products/categories")
@ResponseBody
public List<String> getCategories() {
    return productService.getAllCategories();
}
    // Cập nhật tồn kho
    @PostMapping("/admin/products/update-stock")
    public String updateStock(@RequestParam Long id,
                            @RequestParam Integer quantity,
                            RedirectAttributes redirectAttributes) {
        try {
            if (quantity < 0) {
                redirectAttributes.addFlashAttribute("error", "Số lượng không được âm!");
                return "redirect:/admin/products";
            }
            Product product = productService.updateInventory(id, quantity);
            redirectAttributes.addFlashAttribute("success", "Đã cập nhật tồn kho thành công!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Có lỗi xảy ra khi cập nhật tồn kho: " + e.getMessage());
        }
        return "redirect:/admin/products";
    }

    // Cập nhật tồn kho hàng loạt
    @PostMapping("/admin/products/bulk-update-stock")
    public String bulkUpdateStock(@RequestParam("ids") List<Long> ids,
                                @RequestParam Integer quantity,
                                RedirectAttributes redirectAttributes) {
        try {
            if (quantity < 0) {
                redirectAttributes.addFlashAttribute("error", "Số lượng không được âm!");
                return "redirect:/admin/products";
            }

            int updatedCount = 0;
            for (Long id : ids) {
                Optional<Product> productOpt = productService.getProductById(id);
                if (productOpt.isPresent()) {
                    productService.updateInventory(id, quantity);
                    updatedCount++;
                }
            }
            redirectAttributes.addFlashAttribute("success", 
                "Đã cập nhật tồn kho cho " + updatedCount + " sản phẩm thành công!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", 
                "Có lỗi xảy ra khi cập nhật tồn kho hàng loạt: " + e.getMessage());
        }
        return "redirect:/admin/products";
    }

    // Xóa nhiều sản phẩm
    @PostMapping("/admin/products/bulk-delete")
    public String bulkDelete(@RequestParam("ids") List<Long> ids,
                           RedirectAttributes redirectAttributes) {
        try {
            int deletedCount = 0;
            for (Long id : ids) {
                productService.deleteProduct(id);
                deletedCount++;
            }
            redirectAttributes.addFlashAttribute("success", 
                "Đã xóa " + deletedCount + " sản phẩm thành công!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", 
                "Có lỗi xảy ra khi xóa sản phẩm: " + e.getMessage());
        }
        return "redirect:/admin/products";
    }

    // ==================== USER PRODUCT ENDPOINTS ====================
    // Loại bỏ các endpoint user conflicting, để UserController xử lý

    // Tìm kiếm sản phẩm cho user
    @GetMapping("/products/search")
    public String searchProducts(@RequestParam String q,
                                @RequestParam(defaultValue = "0") int page,
                                @RequestParam(defaultValue = "12") int size,
                                Model model) {

        Pageable pageable = PageRequest.of(page, size, Sort.by("id").descending());
        Page<Product> productPage = productService.findByNameContainingIgnoreCaseAndQuantityGreaterThan(q, 0, pageable);

        model.addAttribute("products", productPage.getContent());
        model.addAttribute("searchQuery", q);
        model.addAttribute("currentPage", page);
        model.addAttribute("totalPages", productPage.getTotalPages());
        model.addAttribute("totalElements", productPage.getTotalElements());

        return "user/search-results";
    }

    // Chi tiết sản phẩm cho user
    @GetMapping("/products/detail/{id}")
    public String viewProductDetail(@PathVariable Long id,
                                   HttpSession session,
                                   Model model) {
        Optional<Product> productOpt = productService.getProductById(id);

        if (productOpt.isPresent()) {
            Product product = productOpt.get();
            model.addAttribute("product", product);

            // Thêm vào danh sách đã xem gần đây
            addToRecentlyViewed(session, product);

            // Sản phẩm liên quan
            List<Product> relatedProducts = productService.findByCategoryAndIdNotAndQuantityGreaterThan(
                product.getCategory(), product.getId(), 0);
            if (relatedProducts.size() > 4) {
                relatedProducts = relatedProducts.subList(0, 4);
            }
            model.addAttribute("relatedProducts", relatedProducts);

            return "user/product-detail";
        }

        return "error/404";
    }

    // Thêm vào giỏ hàng
    @PostMapping("/cart/add/{id}")
    public String addToCart(@PathVariable Long id,
                           @RequestParam(defaultValue = "1") Integer quantity,
                           HttpSession session,
                           RedirectAttributes redirectAttributes) {
        try {
            Optional<Product> productOpt = productService.getProductById(id);

            if (productOpt.isPresent()) {
                Product product = productOpt.get();

                if (product.getQuantity() >= quantity) {
                    @SuppressWarnings("unchecked")
                    List<CartItem> cart = (List<CartItem>) session.getAttribute("cart");
                    if (cart == null) {
                        cart = new ArrayList<>();
                    }

                    boolean found = false;
                    for (CartItem item : cart) {
                        if (item.getProductId().equals(id)) {
                            item.setQuantity(item.getQuantity() + quantity);
                            found = true;
                            break;
                        }
                    }

                    if (!found) {
                        cart.add(new CartItem(id, product.getName(), quantity, product.getPrice()));
                    }

                    session.setAttribute("cart", cart);

                    redirectAttributes.addFlashAttribute("success",
                        "Đã thêm '" + product.getName() + "' vào giỏ hàng!");
                } else {
                    redirectAttributes.addFlashAttribute("error",
                        "Sản phẩm không đủ số lượng trong kho!");
                }
            } else {
                redirectAttributes.addFlashAttribute("error", "Không tìm thấy sản phẩm!");
            }
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error",
                "Có lỗi xảy ra khi thêm vào giỏ hàng: " + e.getMessage());
        }

        return "redirect:/products/detail/" + id;
    }

    // ==================== SHARED ENDPOINTS ====================

    @GetMapping("/dashboard")
    public String dashboard(HttpSession session) {
        String role = (String) session.getAttribute("userRole");

        if ("ADMIN".equals(role)) {
            return "redirect:/admin/dashboard";
        } else {
            return "redirect:/user/dashboard";
        }
    }

    // ==================== HELPER METHODS ====================

    private void addToRecentlyViewed(HttpSession session, Product product) {
        @SuppressWarnings("unchecked")
        List<Product> recentlyViewed = (List<Product>) session.getAttribute("recentlyViewed");

        if (recentlyViewed == null) {
            recentlyViewed = new ArrayList<>();
        }

        recentlyViewed.removeIf(p -> p.getId().equals(product.getId()));
        recentlyViewed.add(0, product);

        if (recentlyViewed.size() > 5) {
            recentlyViewed = recentlyViewed.subList(0, 5);
        }

        session.setAttribute("recentlyViewed", recentlyViewed);
    }

    private String saveImage(MultipartFile file) throws IOException {
        String uploadDir = "uploads/products/";
        String fileName = System.currentTimeMillis() + "_" + file.getOriginalFilename();
        
        // Tạo thư mục nếu chưa tồn tại
        File dir = new File(uploadDir);
        if (!dir.exists()) {
            dir.mkdirs();
        }

        // Lưu file
        File dest = new File(uploadDir + fileName);
        file.transferTo(dest);
        
        return "/" + uploadDir + fileName;
    }

    // ==================== DTO CLASSES ====================

    public static class CartItem {
        private Long productId;
        private String productName;
        private Integer quantity;
        private Double price;

        public CartItem(Long productId, String productName, Integer quantity, Double price) {
            this.productId = productId;
            this.productName = productName;
            this.quantity = quantity;
            this.price = price;
        }

        // Getters and Setters
        public Long getProductId() { return productId; }
        public void setProductId(Long productId) { this.productId = productId; }
        public String getProductName() { return productName; }
        public void setProductName(String productName) { this.productName = productName; }
        public Integer getQuantity() { return quantity; }
        public void setQuantity(Integer quantity) { this.quantity = quantity; }
        public Double getPrice() { return price; }
        public void setPrice(Double price) { this.price = price; }
    }
}