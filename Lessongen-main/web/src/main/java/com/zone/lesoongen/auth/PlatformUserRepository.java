package com.zone.lesoongen.auth;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class PlatformUserRepository {
    private final JdbcTemplate db;

    public PlatformUserRepository(JdbcTemplate db) {
        this.db = db;
    }

    public Optional<PlatformUser> findByUsername(String username) {
        List<PlatformUser> rows = db.query("""
                SELECT id, username, password_hash, display_name, role, status,
                       created_at, updated_at, last_login_at
                FROM platform_user WHERE username = ?
                """, (rs, row) -> map(rs), username);
        return rows.stream().findFirst();
    }

    public Optional<PlatformUser> findById(String id) {
        List<PlatformUser> rows = db.query("""
                SELECT id, username, password_hash, display_name, role, status,
                       created_at, updated_at, last_login_at
                FROM platform_user WHERE id = ?
                """, (rs, row) -> map(rs), id);
        return rows.stream().findFirst();
    }

    public void insert(PlatformUser user) {
        db.update("""
                INSERT INTO platform_user
                (id, username, password_hash, display_name, role, status,
                 created_at, updated_at, last_login_at)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)
                """, user.id(), user.username(), user.passwordHash(), user.displayName(),
                user.role(), user.status(), Timestamp.from(user.createdAt()),
                Timestamp.from(user.updatedAt()), null);
    }

    public void updateLastLogin(String userId, Instant now) {
        db.update("UPDATE platform_user SET last_login_at = ?, updated_at = ? WHERE id = ?",
                Timestamp.from(now), Timestamp.from(now), userId);
    }

    private static PlatformUser map(ResultSet rs) throws SQLException {
        Timestamp lastLogin = rs.getTimestamp("last_login_at");
        return new PlatformUser(rs.getString("id"), rs.getString("username"),
                rs.getString("password_hash"), rs.getString("display_name"),
                rs.getString("role"), rs.getString("status"),
                rs.getTimestamp("created_at").toInstant(),
                rs.getTimestamp("updated_at").toInstant(),
                lastLogin == null ? null : lastLogin.toInstant());
    }
}
