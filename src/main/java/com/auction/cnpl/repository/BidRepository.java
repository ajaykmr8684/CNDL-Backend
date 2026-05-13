package com.auction.cnpl.repository;

import com.auction.cnpl.model.Bid;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface BidRepository extends JpaRepository<Bid, Long> {
    List<Bid> findByPlayerIdOrderByAmountDesc(Long playerId);
}