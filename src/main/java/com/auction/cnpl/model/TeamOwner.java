package com.auction.cnpl.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.persistence.*;

@Entity
@Table(name = "team_owners")
@Data
@NoArgsConstructor
public class TeamOwner {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String name;
    private String username;

    private String password;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    @ManyToOne
    @JoinColumn(name = "team_id")
    @JsonIgnore
    private Team team;

    public void setTeam(Team team) {
        this.team = team;
    }

    public Team getTeam() {
        return this.team;
    }
}