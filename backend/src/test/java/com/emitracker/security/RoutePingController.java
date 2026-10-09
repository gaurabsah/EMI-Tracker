package com.emitracker.security;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

/** Test-only stand-ins for role-protected routes, so S1 can verify 401/403 before the real endpoints exist. */
@RestController
class RoutePingController {

    @GetMapping("/api/admin/ping")
    String admin() {
        return "ok";
    }

    @GetMapping("/api/plans/ping")
    String company() {
        return "ok";
    }

    @GetMapping("/api/me/ping")
    String customer() {
        return "ok";
    }
}
