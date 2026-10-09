package com.emitracker.company;

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
public class CompanyService {

    public record CompanyView(Long id, String name, boolean active, String email) {
    }

    private final CompanyRepository companies;
    private final UserRepository users;
    private final PasswordEncoder encoder;

    public CompanyService(CompanyRepository companies, UserRepository users, PasswordEncoder encoder) {
        this.companies = companies;
        this.users = users;
        this.encoder = encoder;
    }

    @Transactional
    public CompanyView create(String name, String email, String password) {
        String normalised = email.trim();
        if (users.existsByEmailIgnoreCase(normalised)) {
            throw ApiException.conflict("Email already in use");
        }
        Company company = companies.save(new Company(name.trim()));
        users.save(new User(normalised, encoder.encode(password), Role.COMPANY, company.getId()));
        return new CompanyView(company.getId(), company.getName(), company.isActive(), normalised);
    }

    @Transactional(readOnly = true)
    public List<CompanyView> list() {
        Map<Long, String> emailByCompany = users.findByRole(Role.COMPANY).stream()
                .collect(Collectors.toMap(User::getCompanyId, User::getEmail, (a, b) -> a));
        return companies.findAllByOrderByIdAsc().stream()
                .map(c -> new CompanyView(c.getId(), c.getName(), c.isActive(), emailByCompany.get(c.getId())))
                .toList();
    }

    @Transactional
    public CompanyView setActive(Long id, boolean active) {
        Company company = companies.findById(id).orElseThrow(() -> ApiException.notFound("Company not found"));
        company.setActive(active);
        String email = users.findByRole(Role.COMPANY).stream()
                .filter(u -> id.equals(u.getCompanyId())).map(User::getEmail).findFirst().orElse(null);
        return new CompanyView(company.getId(), company.getName(), company.isActive(), email);
    }
}
