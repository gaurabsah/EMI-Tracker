package com.emitracker.user;

import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import com.emitracker.config.AppProperties;

@Component
@Order(1)
public class AdminSeeder implements ApplicationRunner {

    private final UserRepository users;
    private final PasswordEncoder encoder;
    private final AppProperties props;

    public AdminSeeder(UserRepository users, PasswordEncoder encoder, AppProperties props) {
        this.users = users;
        this.encoder = encoder;
        this.props = props;
    }

    @Override
    public void run(ApplicationArguments args) {
        String email = props.admin().email();
        if (!users.existsByEmailIgnoreCase(email)) {
            users.save(new User(email, encoder.encode(props.admin().password()), Role.ADMIN, null));
        }
    }
}
