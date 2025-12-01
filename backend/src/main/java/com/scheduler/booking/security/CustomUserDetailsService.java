package com.scheduler.booking.security;

import com.scheduler.booking.model.Admin;
import com.scheduler.booking.model.BusinessUser;
import com.scheduler.booking.model.Customer;
import com.scheduler.booking.repository.AdminRepository;
import com.scheduler.booking.repository.BusinessUserRepository;
import com.scheduler.booking.repository.CustomerRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class CustomUserDetailsService implements UserDetailsService {

    private final AdminRepository adminRepository;
    private final BusinessUserRepository businessUserRepository;
    private final CustomerRepository customerRepository;

    @Override
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        // Try admin first
        Optional<Admin> admin = adminRepository.findByEmail(email);
        if (admin.isPresent()) {
            return new User(
                    admin.get().getEmail(),
                    admin.get().getPasswordHash(),
                    Collections.singletonList(new SimpleGrantedAuthority("ROLE_ADMIN"))
            );
        }

        // Try business user
        Optional<BusinessUser> businessUser = businessUserRepository.findByEmail(email);
        if (businessUser.isPresent()) {
            return new User(
                    businessUser.get().getEmail(),
                    businessUser.get().getPasswordHash(),
                    Collections.singletonList(new SimpleGrantedAuthority("ROLE_BUSINESS"))
            );
        }

        // Try customer
        Optional<Customer> customer = customerRepository.findByEmail(email);
        if (customer.isPresent() && customer.get().getPasswordHash() != null) {
            return new User(
                    customer.get().getEmail(),
                    customer.get().getPasswordHash(),
                    Collections.singletonList(new SimpleGrantedAuthority("ROLE_CUSTOMER"))
            );
        }

        throw new UsernameNotFoundException("User not found with email: " + email);
    }
}
