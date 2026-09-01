package com.se196693.mvc.repository;

import com.se196693.mvc.dto.response.UserResponse;
import com.se196693.mvc.entity.User;
import com.se196693.mvc.enums.AuthProvider;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long>, JpaSpecificationExecutor<User> {
    /*
    - JpaSpecificationExecutor<> interface cung cấp bới SDJ
    - cung cấp sẵn các hàm để thực thi các truy vấn dữ liệu dựa trên điều kiện động --> SPECIFICATION
    - Cách hoạt động: hoạt động dụa trên khái niệm Specification<T>. Mỗi đối tượng S
      đại diện cho 1 điều kiện riêng lẻ (username bằng X, status bằng Y)
    - Cơ chế: nối các S lại với nhau bằng các toán tử logic AND OR NOT
        ví dụ các hàm có sẵn trong interface này:
        List<T> findAll(Specification<T> spec)
        Page<T> findAll(Specification<T> spec, Pageable pageable)
     */
    Optional<User> findByUsername(String username);

    boolean existsByUsername(String username);

    boolean existsByEmail(String email);

    List<User> findUserByUsernameOrFullNameContainingIgnoreCase(String username, String fullName);

    Optional<User> findByEmail(String email);

    Optional<User> findByAuthProviderAndProviderId(
            AuthProvider provider,
            String providerId
    );

}
