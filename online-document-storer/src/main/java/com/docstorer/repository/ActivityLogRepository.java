package com.docstorer.repository;

import com.docstorer.entity.ActivityLog;
import com.docstorer.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ActivityLogRepository extends JpaRepository<ActivityLog, Long> {
    Page<ActivityLog> findByUser(User user, Pageable pageable);
    Page<ActivityLog> findAllByOrderByCreatedAtDesc(Pageable pageable);
    List<ActivityLog> findTop10ByUserOrderByCreatedAtDesc(User user);

    @Query("SELECT COUNT(a) FROM ActivityLog a WHERE a.action = 'LOGIN'")
    long countLogins();
}
