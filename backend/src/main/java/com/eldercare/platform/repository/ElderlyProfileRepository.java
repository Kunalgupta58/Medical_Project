package com.eldercare.platform.repository;

import com.eldercare.platform.entity.ElderlyProfile;
import com.eldercare.platform.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface ElderlyProfileRepository extends JpaRepository<ElderlyProfile, UUID> {
    Optional<ElderlyProfile> findByUser(User user);
    Optional<ElderlyProfile> findByUserId(UUID userId);
}
