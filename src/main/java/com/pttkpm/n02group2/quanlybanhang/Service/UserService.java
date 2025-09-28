package com.pttkpm.n02group2.quanlybanhang.Service;

import com.pttkpm.n02group2.quanlybanhang.Model.User;
import com.pttkpm.n02group2.quanlybanhang.Repository.UserRepository;

import jakarta.annotation.PostConstruct;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class UserService {

    @Autowired
    private UserRepository userRepository;

    // Đăng ký user mới
    public User registerUser(User user) {
        // Kiểm tra username đã tồn tại
        if (userRepository.existsByUsername(user.getUsername())) {
            throw new RuntimeException("Username đã tồn tại!");
        }
        
        // Kiểm tra email đã tồn tại
        if (userRepository.existsByEmail(user.getEmail())) {
            throw new RuntimeException("Email đã tồn tại!");
        }
        
        // Mã hóa password (đơn giản, nên dùng BCrypt trong thực tế)
        user.setPassword(encodePassword(user.getPassword()));
        
        // Mặc định role là USER
        if (user.getRole() == null) {
            user.setRole(User.Role.USER);
        }
        
        return userRepository.save(user);
    }

    // Đăng nhập
    public Optional<User> authenticate(String username, String password) {
        Optional<User> userOpt = userRepository.findByUsernameOrEmail(username, username);
        
        if (userOpt.isPresent()) {
            User user = userOpt.get();
            // Kiểm tra password và trạng thái active
            if (user.getActive() && checkPassword(password, user.getPassword())) {
                return Optional.of(user);
            }
        }
        return Optional.empty();
    }
@PostConstruct
public void initDemoData() {
    // Tạo demo admin accounts
    if (!userRepository.existsByUsername("admin")) {
        User admin = new User("admin", "admin123", "admin@example.com", "Quản trị viên", User.Role.ADMIN);
        registerUser(admin);
    }
    
    if (!userRepository.existsByUsername("manager")) {
        User manager = new User("manager", "manager123", "manager@example.com", "Quản lý", User.Role.ADMIN);
        registerUser(manager);
    }
    
    // Tạo demo user accounts
    if (!userRepository.existsByUsername("user1")) {
        User user1 = new User("user1", "123456", "user1@example.com", "Người dùng 1", User.Role.USER);
        registerUser(user1);
    }
    
    if (!userRepository.existsByUsername("customer")) {
        User customer = new User("customer", "123456", "customer@example.com", "Khách hàng", User.Role.USER);
        registerUser(customer);
    }
}
    // Lấy tất cả users
    public List<User> getAllUsers() {
        return userRepository.findAll();
    }

    // Lấy user theo ID
    public Optional<User> getUserById(Long id) {
        return userRepository.findById(id);
    }

    // Lấy user theo username
    public Optional<User> getUserByUsername(String username) {
        return userRepository.findByUsername(username);
    }

    // Cập nhật user
    public Optional<User> updateUser(Long id, User userDetails) {
        Optional<User> userOpt = getUserById(id);
        if (userOpt.isPresent()) {
            User user = userOpt.get();
            
            user.setFullName(userDetails.getFullName());
            user.setEmail(userDetails.getEmail());
            user.setPhone(userDetails.getPhone());
            
            // Chỉ admin mới có thể thay đổi role
            if (userDetails.getRole() != null) {
                user.setRole(userDetails.getRole());
            }
            
            return Optional.of(userRepository.save(user));
        }
        return Optional.empty();
    }

    // Thay đổi password
    public boolean changePassword(Long userId, String oldPassword, String newPassword) {
        Optional<User> userOpt = getUserById(userId);
        if (userOpt.isPresent()) {
            User user = userOpt.get();
            if (checkPassword(oldPassword, user.getPassword())) {
                user.setPassword(encodePassword(newPassword));
                userRepository.save(user);
                return true;
            }
        }
        return false;
    }

    // Deactivate user
    public boolean deactivateUser(Long id) {
        Optional<User> userOpt = getUserById(id);
        if (userOpt.isPresent()) {
            User user = userOpt.get();
            user.setActive(false);
            userRepository.save(user);
            return true;
        }
        return false;
    }

    // Activate user
    public boolean activateUser(Long id) {
        Optional<User> userOpt = getUserById(id);
        if (userOpt.isPresent()) {
            User user = userOpt.get();
            user.setActive(true);
            userRepository.save(user);
            return true;
        }
        return false;
    }

    // Lấy users theo role
    public List<User> getUsersByRole(User.Role role) {
        return userRepository.findByRole(role);
    }

    // Utility methods
    private String encodePassword(String password) {
        // Đơn giản hóa - trong thực tế nên dùng BCrypt
        return "encoded_" + password;
    }

    private boolean checkPassword(String rawPassword, String encodedPassword) {
        // Đơn giản hóa - trong thực tế nên dùng BCrypt
        return encodedPassword.equals("encoded_" + rawPassword);
    }
}