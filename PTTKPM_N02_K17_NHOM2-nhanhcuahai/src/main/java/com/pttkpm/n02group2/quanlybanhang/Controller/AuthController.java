package com.pttkpm.n02group2.quanlybanhang.Controller;

import com.pttkpm.n02group2.quanlybanhang.Model.User;
import com.pttkpm.n02group2.quanlybanhang.Service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import jakarta.servlet.http.HttpSession;
import java.util.Optional;

@Controller
public class AuthController {

    @Autowired
    private UserService userService;

    // ==================== TRANG CHỦ ====================
    @GetMapping("/")
    public String home(HttpSession session) {
        User user = (User) session.getAttribute("user");
        
        if (user == null) {
            return "redirect:/login";
        }
        
        // Chuyển hướng theo vai trò
        if (user.getRole() == User.Role.ADMIN) {
            return "redirect:/admin/dashboard";
        } else {
            return "redirect:/user/pos";
        }
    }

    // MAPPING CHO /login (không có prefix)
    @GetMapping("/login")
    public String showLoginForm(Model model, HttpSession session) {
        // Nếu đã đăng nhập, chuyển về trang chủ
        if (session.getAttribute("username") != null) {
            return "redirect:/";
        }
        return "auth/login";
    }

    @PostMapping("/login")
    public String processLogin(@RequestParam String username, 
                             @RequestParam String password,
                             @RequestParam(required = false, defaultValue = "user") String userType,
                             HttpSession session, 
                             RedirectAttributes redirectAttributes) {
        try {
            Optional<User> userOpt = userService.authenticate(username, password);
            
            if (userOpt.isPresent()) {
                User user = userOpt.get();
                
                // PHÂN QUYỀN: Kiểm tra userType có khớp với role không
                if ("admin".equals(userType) && user.getRole() != User.Role.ADMIN) {
                    redirectAttributes.addFlashAttribute("error", 
                        "Tài khoản này không có quyền quản trị viên!");
                    return "redirect:/login";
                }
                if ("user".equals(userType) && user.getRole() != User.Role.USER) {
                    redirectAttributes.addFlashAttribute("error", 
                        "Tài khoản này không phải là tài khoản người dùng thường!");
                    return "redirect:/login";
                }
                
                // Set session attributes
                session.setAttribute("user", user);
                session.setAttribute("username", user.getUsername());
                session.setAttribute("userRole", user.getRole().name());
                session.setAttribute("userId", user.getId());
                
                // Thông báo đăng nhập thành công
                String roleText = user.getRole() == User.Role.ADMIN ? "Quản trị viên" : "Người dùng";
                redirectAttributes.addFlashAttribute("success", 
                    "Đăng nhập thành công với quyền " + roleText + "!");
                
                // Redirect theo vai trò
                if (user.getRole() == User.Role.ADMIN) {
                    return "redirect:/admin/dashboard";  // Admin đến dashboard riêng
                } else {
                    return "redirect:/user/pos";         // User đến POS
                }
            }
            
            redirectAttributes.addFlashAttribute("error", "Tên đăng nhập hoặc mật khẩu không đúng");
            return "redirect:/login";
            
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Có lỗi xảy ra: " + e.getMessage());
            return "redirect:/login";
        }
    }

    @GetMapping("/logout")
    public String logout(HttpSession session, RedirectAttributes redirectAttributes) {
        session.invalidate();
        redirectAttributes.addFlashAttribute("success", "Đăng xuất thành công!");
        return "redirect:/login";
    }

    // MAPPING CHO /auth/* (với prefix)
    @GetMapping("/auth/login")
    public String showAuthLoginForm(Model model, HttpSession session) {
        return showLoginForm(model, session);
    }

    @PostMapping("/auth/login")
    public String processAuthLogin(@RequestParam String username, 
                                 @RequestParam String password,
                                 @RequestParam(required = false, defaultValue = "user") String userType,
                                 HttpSession session,
                                 RedirectAttributes redirectAttributes) {
        return processLogin(username, password, userType, session, redirectAttributes);
    }

    @GetMapping("/auth/register")
    public String showRegisterForm(Model model) {
        model.addAttribute("user", new User());
        return "auth/register";
    }

    @PostMapping("/auth/register")
    public String register(@ModelAttribute User user,
                          @RequestParam String confirmPassword,
                          @RequestParam String role,
                          RedirectAttributes redirectAttributes) {
        try {
            // Kiểm tra mật khẩu xác nhận
            if (!user.getPassword().equals(confirmPassword)) {
                redirectAttributes.addFlashAttribute("error", "Mật khẩu xác nhận không khớp!");
                return "redirect:/auth/register";
            }

            // Đặt role dựa vào lựa chọn của user
            if ("ADMIN".equals(role)) {
                user.setRole(User.Role.ADMIN);
            } else {
                user.setRole(User.Role.USER);
            }

            userService.registerUser(user);
            
            redirectAttributes.addFlashAttribute("success", 
                "Đăng ký thành công với quyền " + user.getRole() + "! Vui lòng đăng nhập.");
            return "redirect:/login";
            
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
            return "redirect:/auth/register";
        }
    }

    @GetMapping("/auth/logout")
    public String authLogout(HttpSession session, RedirectAttributes redirectAttributes) {
        session.invalidate();
        redirectAttributes.addFlashAttribute("success", "Đăng xuất thành công!");
        return "redirect:/login";
    }

    @GetMapping("/auth/profile")
    public String showProfile(Model model, HttpSession session) {
        User user = (User) session.getAttribute("user");
        if (user == null) {
            return "redirect:/login";
        }
        
        // Lấy thông tin user mới nhất từ DB
        Optional<User> userOpt = userService.getUserById(user.getId());
        if (userOpt.isPresent()) {
            model.addAttribute("user", userOpt.get());
        }
        
        return "auth/profile";
    }

    @PostMapping("/auth/profile")
    public String updateProfile(@ModelAttribute User userDetails,
                               HttpSession session,
                               RedirectAttributes redirectAttributes) {
        User sessionUser = (User) session.getAttribute("user");
        if (sessionUser == null) {
            return "redirect:/login";
        }

        try {
            Optional<User> updatedUser = userService.updateUser(sessionUser.getId(), userDetails);
            if (updatedUser.isPresent()) {
                // Cập nhật session
                session.setAttribute("user", updatedUser.get());
                redirectAttributes.addFlashAttribute("success", "Cập nhật thông tin thành công!");
            }
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Có lỗi xảy ra: " + e.getMessage());
        }

        return "redirect:/auth/profile";
    }

    @PostMapping("/auth/change-password")
    public String changePassword(@RequestParam String oldPassword,
                                @RequestParam String newPassword,
                                @RequestParam String confirmPassword,
                                HttpSession session,
                                RedirectAttributes redirectAttributes) {
        User user = (User) session.getAttribute("user");
        if (user == null) {
            return "redirect:/login";
        }

        try {
            if (!newPassword.equals(confirmPassword)) {
                redirectAttributes.addFlashAttribute("error", "Mật khẩu mới không khớp!");
                return "redirect:/auth/profile";
            }

            boolean success = userService.changePassword(user.getId(), oldPassword, newPassword);
            if (success) {
                redirectAttributes.addFlashAttribute("success", "Đổi mật khẩu thành công!");
            } else {
                redirectAttributes.addFlashAttribute("error", "Mật khẩu cũ không đúng!");
            }
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Có lỗi xảy ra: " + e.getMessage());
        }

        return "redirect:/auth/profile";
    }
}