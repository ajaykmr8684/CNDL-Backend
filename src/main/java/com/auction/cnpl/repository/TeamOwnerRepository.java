package com.auction.cnpl.repository;

import com.auction.cnpl.model.TeamOwner;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface TeamOwnerRepository extends JpaRepository<TeamOwner, Long> {
    Optional<TeamOwner> findByUsername(String username);
}