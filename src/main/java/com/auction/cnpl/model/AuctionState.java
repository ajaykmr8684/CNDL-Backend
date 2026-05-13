package com.auction.cnpl.model;

import lombok.Data;
import lombok.NoArgsConstructor;

import javax.persistence.*;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "auction_state")
@Data
@NoArgsConstructor
public class AuctionState {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne
    @JoinColumn(name = "current_player_id")
    private Player currentPlayer;

    @OneToOne
    @JoinColumn(name = "next_player_id")
    private Player nextPlayer;

    @Column(name = "current_player_index")
    private Integer currentPlayerIndex = 0;

    @OneToOne
    @JoinColumn(name = "highest_bid_id")
    private Bid highestBid;

    @Transient // We'll handle notifications separately since they don't need persistence
    private List<String> notifications = new ArrayList<>();

    // Add transient fields for frontend convenience
    @Transient
    private Integer basePrice;

    @Transient
    private Integer stepUpAmount;

    @Transient
    private Integer nextBidAmount;

    // After fetching the state, we'll populate these fields
    public void calculateBidAmounts() {
        if (currentPlayer != null) {
            this.basePrice = currentPlayer.getBasePrice();
            this.stepUpAmount = currentPlayer.getStepUpAmount();

            if (highestBid == null) {
                this.nextBidAmount = this.basePrice;
            } else {
                this.nextBidAmount = currentPlayer.getNextBidAmount(highestBid.getAmount());
            }
        }
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Player getCurrentPlayer() {
        return currentPlayer;
    }

    public void setCurrentPlayer(Player currentPlayer) {
        this.currentPlayer = currentPlayer;
    }

    public Player getNextPlayer() {
        return nextPlayer;
    }

    public void setNextPlayer(Player nextPlayer) {
        this.nextPlayer = nextPlayer;
    }

    public Integer getCurrentPlayerIndex() {
        return currentPlayerIndex;
    }

    public void setCurrentPlayerIndex(Integer currentPlayerIndex) {
        this.currentPlayerIndex = currentPlayerIndex;
    }

    public Bid getHighestBid() {
        return highestBid;
    }

    public void setHighestBid(Bid highestBid) {
        this.highestBid = highestBid;
    }

    public List<String> getNotifications() {
        return notifications;
    }

    public void setNotifications(List<String> notifications) {
        this.notifications = notifications;
    }

    public Integer getBasePrice() {
        if (basePrice == null && currentPlayer != null) {
            basePrice = currentPlayer.getBasePrice();
        }
        return basePrice;
    }

    public void setBasePrice(Integer basePrice) {
        this.basePrice = basePrice;
    }

    public Integer getStepUpAmount() {
        if (stepUpAmount == null && currentPlayer != null) {
            stepUpAmount = currentPlayer.getStepUpAmount();
        }
        return stepUpAmount;
    }

    public void setStepUpAmount(Integer stepUpAmount) {
        this.stepUpAmount = stepUpAmount;
    }

    public Integer getNextBidAmount() {
        if (nextBidAmount == null && currentPlayer != null) {
            if (highestBid == null) {
                nextBidAmount = currentPlayer.getBasePrice();
            } else {
                nextBidAmount = currentPlayer.getNextBidAmount(highestBid.getAmount());
            }
        }
        return nextBidAmount;
    }

    public void setNextBidAmount(Integer nextBidAmount) {
        this.nextBidAmount = nextBidAmount;
    }
}