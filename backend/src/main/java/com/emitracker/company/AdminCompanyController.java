package com.emitracker.company;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

@RestController
@RequestMapping("/api/admin/companies")
public class AdminCompanyController {

    public record CreateCompanyRequest(
            @NotBlank String name,
            @NotBlank @Email String email,
            @NotBlank @Size(min = 8) String password) {
    }

    public record SetActiveRequest(@NotNull Boolean active) {
    }

    private final CompanyService service;

    public AdminCompanyController(CompanyService service) {
        this.service = service;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public CompanyService.CompanyView create(@Valid @RequestBody CreateCompanyRequest r) {
        return service.create(r.name(), r.email(), r.password());
    }

    @GetMapping
    public List<CompanyService.CompanyView> list() {
        return service.list();
    }

    @PatchMapping("/{id}/active")
    public CompanyService.CompanyView setActive(@PathVariable Long id, @Valid @RequestBody SetActiveRequest r) {
        return service.setActive(id, r.active());
    }
}
