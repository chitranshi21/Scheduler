package com.scheduler.booking.service;

import com.scheduler.booking.dto.LoginRequest;
import com.scheduler.booking.dto.LoginResponse;
import com.scheduler.booking.model.Admin;
import com.scheduler.booking.model.BusinessUser;
import com.scheduler.booking.model.Customer;
import com.scheduler.booking.repository.AdminRepository;
import com.scheduler.booking.repository.BusinessUserRepository;
import com.scheduler.booking.repository.CustomerRepository;
import com.scheduler.booking.security.JwtUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final AdminRepository adminRepository;
    private final BusinessUserRepository businessUserRepository;
    private final CustomerRepository customerRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;

    public LoginResponse login(LoginRequest request) {
        String email = request.getEmail();
        String password = request.getPassword();

        // Try admin first
        Optional<Admin> admin = adminRepository.findByEmail(email);
        if (admin.isPresent() && passwordEncoder.matches(password, admin.get().getPasswordHash())) {
            admin.get().setLastLoginAt(LocalDateTime.now());
            adminRepository.save(admin.get());

            String token = jwtUtil.generateToken(email, "ADMIN");
            Map<String, Object> details = new HashMap<>();
            details.put("id", admin.get().getId());
            details.put("name", admin.get().getFirstName() + " " + admin.get().getLastName());

            return new LoginResponse(token, email, "ADMIN", details);
        }

        // Try business user
        Optional<BusinessUser> businessUser = businessUserRepository.findByEmail(email);
        if (businessUser.isPresent() && passwordEncoder.matches(password, businessUser.get().getPasswordHash())) {
            businessUser.get().setLastLoginAt(LocalDateTime.now());
            businessUserRepository.save(businessUser.get());

            String token = jwtUtil.generateToken(email, "BUSINESS");
            Map<String, Object> details = new HashMap<>();
            details.put("id", businessUser.get().getId());
            details.put("tenantId", businessUser.get().getTenantId());
            details.put("name", businessUser.get().getFirstName() + " " + businessUser.get().getLastName());

            return new LoginResponse(token, email, "BUSINESS", details);
        }

        // Try customer
        Optional<Customer> customer = customerRepository.findByEmail(email);
        if (customer.isPresent() && customer.get().getPasswordHash() != null
                && passwordEncoder.matches(password, customer.get().getPasswordHash())) {
            String token = jwtUtil.generateToken(email, "CUSTOMER");
            Map<String, Object> details = new HashMap<>();
            details.put("id", customer.get().getId());
            details.put("name", customer.get().getFullName());

            return new LoginResponse(token, email, "CUSTOMER", details);
        }

        throw new RuntimeException("Invalid email or password");
    }
}
