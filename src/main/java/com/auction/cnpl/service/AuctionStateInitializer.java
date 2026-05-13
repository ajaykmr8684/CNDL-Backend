package com.auction.cnpl.service;

import com.auction.cnpl.model.AuctionState;
import com.auction.cnpl.model.Player;
import com.auction.cnpl.repository.AuctionStateRepository;
import com.auction.cnpl.repository.PlayerRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.PostConstruct;
import java.util.List;

@Service
public class AuctionStateInitializer {

    private final PlayerRepository playerRepository;
    private final AuctionStateRepository auctionStateRepository;

    @Autowired
    public AuctionStateInitializer(
            PlayerRepository playerRepository,
            AuctionStateRepository auctionStateRepository) {
        this.playerRepository = playerRepository;
        this.auctionStateRepository = auctionStateRepository;
    }

    @PostConstruct
    @Transactional
    public void initializeAuctionState() {
        // Only initialize if there's no auction state yet
        if (auctionStateRepository.count() == 0) {
            List<Player> players = playerRepository.findAllByOrderById();
            if (!players.isEmpty()) {
                AuctionState state = new AuctionState();
                state.setCurrentPlayer(players.get(0));
                if (players.size() > 1) {
                    state.setNextPlayer(players.get(1));
                }
                state.setCurrentPlayerIndex(0);
                auctionStateRepository.save(state);

                System.out.println("Auction state initialized with first player: " +
                        players.get(0).getName());
            } else {
                System.out.println("No players found in database. Auction state initialization skipped.");
            }
        } else {
            System.out.println("Auction state already exists. Initialization skipped.");
        }
    }
}