package com.auction.cnpl.model;

import java.util.List;
import java.util.ArrayList;

import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.Fetch;
import org.hibernate.annotations.FetchMode;

import javax.persistence.*;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "teams")
@Data
@NoArgsConstructor
public class Team {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String name;

    @Column(name = "wallet_balance")
    private Long walletBalance = 300000000L;

    @OneToMany(mappedBy = "team", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.EAGER)
    private List<TeamOwner> owners = new ArrayList<>();

    @OneToMany(fetch = FetchType.EAGER)
    @Fetch(value = FetchMode.SUBSELECT)
    @JoinColumn(name = "sold_to_team_id")
    private List<Player> players = new ArrayList<>();

    public List<TeamOwner> getOwners() {
        return owners;
    }

    public void setOwners(List<TeamOwner> owners) {
        this.owners = owners;
    }

    public List<Player> getPlayers() {
        return players;
    }

    public void setPlayers(List<Player> players) {
        this.players = players;
    }

    // Helper method to add owners with proper relationship set
    public void addOwner(TeamOwner owner) {
        owners.add(owner);
        owner.setTeam(this);
    }

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

    public Long getWalletBalance() {
        return walletBalance;
    }

    public void setWalletBalance(Long walletBalance) {
        this.walletBalance = walletBalance;
    }
}