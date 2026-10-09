package com.emitracker.auth;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import com.emitracker.common.ApiException;
import com.emitracker.company.CompanyRepository;
import com.emitracker.customer.Customer;
import com.emitracker.customer.CustomerRepository;
import com.emitracker.security.AuthPrincipal;
import com.emitracker.security.JwtService;
import com.emitracker.user.Role;
import com.emitracker.user.User;
import com.emitracker.user.UserRepository;

@Service
public class AuthService {

    private final UserRepository users;
    private final CompanyRepository companies;
    private final CustomerRepository customers;
    private final PasswordEncoder encoder;
    private final JwtService jwt;

    public AuthService(UserRepository users, CompanyRepository companies, CustomerRepository customers,
            PasswordEncoder encoder, JwtService jwt) {
        this.users = users;
        this.companies = companies;
        this.customers = customers;
        this.encoder = encoder;
        this.jwt = jwt;
    }

    public AuthController.LoginResponse login(String email, String password) {
        User user = users.findByEmailIgnoreCase(email.trim())
                .filter(u -> encoder.matches(password, u.getPasswordHash()))
                .orElseThrow(() -> ApiException.unauthorized("Invalid email or password"));

        if (user.getCompanyId() != null) {
            boolean active = companies.findById(user.getCompanyId()).map(c -> c.isActive()).orElse(false);
            if (!active) {
                throw ApiException.unauthorized("Account is deactivated");
            }
        }

        Long customerId = null;
        if (user.getRole() == Role.CUSTOMER) {
            customerId = customers.findByUserId(user.getId()).map(Customer::getId).orElse(null);
        }

        AuthPrincipal principal = new AuthPrincipal(user.getId(), user.getRole(), user.getCompanyId(), customerId);
        return new AuthController.LoginResponse(jwt.issue(principal), user.getRole().name());
    }
}
