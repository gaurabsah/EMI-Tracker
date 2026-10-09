package com.emitracker.customer;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.emitracker.security.AuthPrincipal;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@RestController
@RequestMapping("/api/customers")
public class CustomerController {

    public record CreateCustomerRequest(
            @NotBlank String name,
            String phone,
            @NotBlank @Email String email,
            @NotBlank @Size(min = 8) String password) {
    }

    private final CustomerService service;

    public CustomerController(CustomerService service) {
        this.service = service;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public CustomerService.CustomerView create(@AuthenticationPrincipal AuthPrincipal me,
            @Valid @RequestBody CreateCustomerRequest r) {
        return service.create(me.companyId(), r.name(), r.phone(), r.email(), r.password());
    }

    @GetMapping
    public List<CustomerService.CustomerView> list(@AuthenticationPrincipal AuthPrincipal me) {
        return service.list(me.companyId());
    }
}
