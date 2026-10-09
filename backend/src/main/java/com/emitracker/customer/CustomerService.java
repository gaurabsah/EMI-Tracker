package com.emitracker.customer;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.emitracker.common.ApiException;
import com.emitracker.user.Role;
import com.emitracker.user.User;
import com.emitracker.user.UserRepository;

@Service
public class CustomerService {

    public record CustomerView(Long id, String name, String phone, String email) {
    }

    private final CustomerRepository customers;
    private final UserRepository users;
    private final PasswordEncoder encoder;

    public CustomerService(CustomerRepository customers, UserRepository users, PasswordEncoder encoder) {
        this.customers = customers;
        this.users = users;
        this.encoder = encoder;
    }

    @Transactional
    public CustomerView create(Long companyId, String name, String phone, String email, String password) {
        String normalised = email.trim();
        if (users.existsByEmailIgnoreCase(normalised)) {
            throw ApiException.conflict("Email already in use");
        }
        User user = users.save(new User(normalised, encoder.encode(password), Role.CUSTOMER, companyId));
        Customer customer = customers.save(new Customer(user.getId(), companyId, name.trim(), phone));
        return new CustomerView(customer.getId(), customer.getName(), customer.getPhone(), normalised);
    }

    @Transactional(readOnly = true)
    public List<CustomerView> list(Long companyId) {
        List<Customer> mine = customers.findByCompanyIdOrderByIdAsc(companyId);
        Map<Long, String> emailByUser = users.findAllById(mine.stream().map(Customer::getUserId).toList()).stream()
                .collect(Collectors.toMap(User::getId, User::getEmail, (a, b) -> a, java.util.HashMap::new));
        return mine.stream()
                .map(c -> new CustomerView(c.getId(), c.getName(), c.getPhone(), emailByUser.get(c.getUserId())))
                .toList();
    }
}
