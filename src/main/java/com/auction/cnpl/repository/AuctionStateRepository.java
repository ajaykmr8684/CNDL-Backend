package com.auction.cnpl.repository;

import com.auction.cnpl.model.AuctionState;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface AuctionStateRepository extends JpaRepository<AuctionState, Long> {
}