package com.pttkpm.n02group2.quanlybanhang.Repository;

import com.pttkpm.n02group2.quanlybanhang.Model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {
    
    // Tìm user theo username
    Optional<User> findByUsername(String username);
    
    // Tìm user theo email
    Optional<User> findByEmail(String email);
    
    // Tìm user theo username hoặc email
    Optional<User> findByUsernameOrEmail(String username, String email);
    
    // Tìm user theo role
    List<User> findByRole(User.Role role);
    
    // Tìm user đang hoạt động
    List<User> findByActiveTrue();
    
    // Kiểm tra username đã tồn tại
    boolean existsByUsername(String username);
    
    // Kiểm tra email đã tồn tại
    boolean existsByEmail(String email);
}