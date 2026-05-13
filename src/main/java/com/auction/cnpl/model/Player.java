package com.auction.cnpl.model;

import lombok.Data;
import lombok.NoArgsConstructor;

import javax.persistence.*;

@Entity
@Table(name = "players")
@Data
@NoArgsConstructor
public class Player {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String name;
    private String photoUrl;
    private String playerType;

    private String tier;

    private String lastYearTeam;
    private Integer runs;

    public Integer getLastYearRanking() {
        return lastYearRanking;
    }

    public void setLastYearRanking(Integer lastYearRanking) {
        this.lastYearRanking = lastYearRanking;
    }

    @Transient
    private Integer lastYearRanking;

    public Integer getAuctionSlot() {
        return auctionSlot;
    }

    public void setAuctionSlot(Integer auctionSlot) {
        this.auctionSlot = auctionSlot;
    }

    @Transient
    private Integer auctionSlot;

    private String battingStat;
    private String bowlingStat;
    private Boolean sold = false;

    @Column(name = "sold_to_team_id")
    private Long soldToTeamId;

    @Column(name = "sold_amount")
    private Integer soldAmount;

    // Method to get base price based on tier
    public Integer getBasePrice() {
        switch (tier) {
            case "Marque":
                return 20000000; // 2,00,00,000
            case "Tier-1":
                return 10000000; // 1,00,00,000
            case "Tier-2":
                return 5000000;  // 50,00,000
            default:
                return 5000000;  // Default to Tier-2 if not specified
        }
    }

    // Method to get step-up amount based on tier
    public Integer getStepUpAmount() {
        switch (tier) {
            case "Marque":
                return 2000000;  // 20,00,000
            case "Tier-1":
                return 500000;   // 5,00,000
            case "Tier-2":
                return 300000;   // 3,00,000
            default:
                return 2000000;   // Default to Tier-2 if not specified
        }
    }

    // Calculate the next bid amount based on current highest bid or base price
    public Integer getNextBidAmount(Integer currentHighestBid) {
        if (currentHighestBid == null || currentHighestBid == 0) {
            return getBasePrice();
        }
        return currentHighestBid + getStepUpAmount();
    }

    // Existing getters and setters...
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getPhotoUrl() {
        return photoUrl;
    }

    public void setPhotoUrl(String photoUrl) {
        this.photoUrl = photoUrl;
    }

    public String getPlayerType() {
        return playerType;
    }

    public void setPlayerType(String playerType) {
        this.playerType = playerType;
    }

    public String getTier() {
        return tier;
    }

    public void setTier(String tier) {
        this.tier = tier;
    }

    public String getLastYearTeam() {
        return lastYearTeam;
    }

    public void setLastYearTeam(String lastYearTeam) {
        this.lastYearTeam = lastYearTeam;
    }

    public Integer getRuns() {
        return runs;
    }

    public void setRuns(Integer runs) {
        this.runs = runs;
    }

    public String getBattingStat() {
        return battingStat;
    }

    public void setBattingStat(String battingStat) {
        this.battingStat = battingStat;
    }

    public String getBowlingStat() {
        return bowlingStat;
    }

    public void setBowlingStat(String bowlingStat) {
        this.bowlingStat = bowlingStat;
    }

    public Boolean getSold() {
        return sold;
    }

    public void setSold(Boolean sold) {
        this.sold = sold;
    }

    public Long getSoldToTeamId() {
        return soldToTeamId;
    }

    public void setSoldToTeamId(Long soldToTeamId) {
        this.soldToTeamId = soldToTeamId;
    }

    public Integer getSoldAmount() {
        return soldAmount;
    }

    public void setSoldAmount(Integer soldAmount) {
        this.soldAmount = soldAmount;
    }
}