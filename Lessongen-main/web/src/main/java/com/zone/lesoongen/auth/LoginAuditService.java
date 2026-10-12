package com.zone.lesoongen.auth;

import java.sql.Timestamp;
import java.time.Clock;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

@Service
public class LoginAuditService {
    private final JdbcTemplate db;
    private final Clock clock;

    public LoginAuditService(JdbcTemplate db, Clock clock) {
        this.db = db;
        this.clock = clock;
    }

    public void record(String userId, String username, boolean success, String clientIp) {
        db.update("""
                INSERT INTO platform_login_audit
                (user_id, username_attempted, success, client_ip, created_at)
                VALUES (?, ?, ?, ?, ?)
                """, userId, username == null ? "" : username, success, clientIp,
                Timestamp.from(clock.instant()));
    }
}
