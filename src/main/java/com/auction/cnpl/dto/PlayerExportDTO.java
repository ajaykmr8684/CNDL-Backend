package com.auction.cnpl.dto;

import com.auction.cnpl.model.Player;

/**
 * Data Transfer Object for Player exports that excludes certain fields
 */
public class PlayerExportDTO {
    private Long id;
    private String name;
    private String tier;
    private String playerType;
    private String battingStat;
    private String bowlingStat;
    private Integer runs;
    private String lastYearTeam;
    private String photoUrl;
    private Boolean sold;
    private Integer soldAmount;
    private Long soldToTeamId;

    // Constructors
    public PlayerExportDTO() {
    }

    public PlayerExportDTO(Player player) {
        this.id = player.getId();
        this.name = player.getName();
        this.tier = player.getTier();
        this.playerType = player.getPlayerType();
        this.battingStat = player.getBattingStat();
        this.bowlingStat = player.getBowlingStat();
        this.runs = player.getRuns();
        this.lastYearTeam = player.getLastYearTeam();
        this.photoUrl = player.getPhotoUrl();
        this.sold = player.getSold();
        this.soldAmount = player.getSoldAmount();
        this.soldToTeamId = player.getSoldToTeamId();
    }

    // Getters and Setters
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

    public String getTier() {
        return tier;
    }

    public void setTier(String tier) {
        this.tier = tier;
    }

    public String getPlayerType() {
        return playerType;
    }

    public void setPlayerType(String playerType) {
        this.playerType = playerType;
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

    public Integer getRuns() {
        return runs;
    }

    public void setRuns(Integer runs) {
        this.runs = runs;
    }

    public String getLastYearTeam() {
        return lastYearTeam;
    }

    public void setLastYearTeam(String lastYearTeam) {
        this.lastYearTeam = lastYearTeam;
    }

    public String getPhotoUrl() {
        return photoUrl;
    }

    public void setPhotoUrl(String photoUrl) {
        this.photoUrl = photoUrl;
    }

    public Boolean getSold() {
        return sold;
    }

    public void setSold(Boolean sold) {
        this.sold = sold;
    }

    public Integer getSoldAmount() {
        return soldAmount;
    }

    public void setSoldAmount(Integer soldAmount) {
        this.soldAmount = soldAmount;
    }

    public Long getSoldToTeamId() {
        return soldToTeamId;
    }

    public void setSoldToTeamId(Long soldToTeamId) {
        this.soldToTeamId = soldToTeamId;
    }
}