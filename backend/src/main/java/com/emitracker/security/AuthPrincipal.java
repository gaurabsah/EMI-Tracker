package com.emitracker.security;

import com.emitracker.user.Role;

/** Identity taken from the JWT. companyId is null for Admin; customerId is set only for Customers. */
public record AuthPrincipal(long userId, Role role, Long companyId, Long customerId) {
}
