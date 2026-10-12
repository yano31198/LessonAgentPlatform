package com.zone.lesoongen.auth;

import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import com.zone.lesoongen.application.AppException;

@Service
public class PlatformAccessService {
    private final JdbcTemplate db;
    private final CurrentUserService currentUser;

    public PlatformAccessService(JdbcTemplate db, CurrentUserService currentUser) {
        this.db = db;
        this.currentUser = currentUser;
    }

    public String currentUserId() {
        return currentUser.require().id();
    }

    public void requireOwnedLesson(String lessonId) {
        Integer count = db.queryForObject("""
                SELECT COUNT(*) FROM platform_lesson WHERE id = ? AND owner_user_id = ?
                """, Integer.class, lessonId, currentUserId());
        if (count == null || count == 0) {
            // Do not disclose whether another user's lesson exists.
            throw new AppException(HttpStatus.NOT_FOUND, "RESOURCE_NOT_FOUND", "教案不存在");
        }
    }
}
