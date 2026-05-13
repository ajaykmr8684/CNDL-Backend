package com.auction.cnpl.service;

import com.auction.cnpl.model.AuctionState;
import com.auction.cnpl.model.Player;
import com.auction.cnpl.model.Team;
import com.auction.cnpl.model.TeamOwner;
import com.auction.cnpl.repository.AuctionStateRepository;
import com.auction.cnpl.repository.PlayerRepository;
import com.auction.cnpl.repository.TeamRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.io.ClassPathResource;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;

@Service
public class ConfigService implements CommandLineRunner {
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final PlayerRepository playerRepository;
    private final TeamRepository teamRepository;
    private final AuctionStateRepository auctionStateRepository;
    private final PasswordEncoder passwordEncoder;

    @Autowired
    public ConfigService(
            PlayerRepository playerRepository,
            TeamRepository teamRepository,
            AuctionStateRepository auctionStateRepository,
            PasswordEncoder passwordEncoder) {
        this.playerRepository = playerRepository;
        this.teamRepository = teamRepository;
        this.auctionStateRepository = auctionStateRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    @Transactional
    public void run(String... args) throws Exception {
        // Only initialize if the database is empty
        if (playerRepository.count() == 0 && teamRepository.count() == 0) {
            initializeDatabase();
        }
    }

    @Transactional
    public void initializeDatabase() throws IOException {
        // Load and save teams
        List<Team> teams = loadTeamsFromJson();

        // Hash passwords and set up relationships
        for (Team team : teams) {
            for (TeamOwner owner : team.getOwners()) {
                // Hash password
                owner.setPassword(passwordEncoder.encode(owner.getPassword()));
                // Connect owner to team
                owner.setTeam(team);
            }
        }

        teamRepository.saveAll(teams);

        // Load and save players
        List<Player> players = loadPlayersFromJson();
        playerRepository.saveAll(players);

        // Initialize auction state with first player
        if (auctionStateRepository.count() == 0 && !players.isEmpty()) {
            AuctionState state = new AuctionState();
            state.setCurrentPlayer(players.get(0));
            if (players.size() > 1) {
                state.setNextPlayer(players.get(1));
            }
            state.setCurrentPlayerIndex(0);
            auctionStateRepository.save(state);

            System.out.println("Auction state initialized with first player: " +
                    players.get(0).getName() + " during database initialization");
        }
    }

    private List<Team> loadTeamsFromJson() throws IOException {
        return Arrays.asList(objectMapper.readValue(
                new ClassPathResource("teams.json").getInputStream(),
                Team[].class
        ));
    }

    private List<Player> loadPlayersFromJson() throws IOException {
        return Arrays.asList(objectMapper.readValue(
                new ClassPathResource("players.json").getInputStream(),
                Player[].class
        ));
    }
}