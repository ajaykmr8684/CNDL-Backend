package com.auction.cnpl.service;

import com.auction.cnpl.dto.PlayerExportDTO;
import com.auction.cnpl.model.*;
import com.auction.cnpl.repository.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class AuctionService {
    private final PlayerRepository playerRepository;
    private final TeamRepository teamRepository;
    private final BidRepository bidRepository;
    private final AuctionStateRepository auctionStateRepository;
    private final SimpMessagingTemplate messagingTemplate;

    private List<String> notifications = new ArrayList<>();

    @Autowired
    public AuctionService(
            PlayerRepository playerRepository,
            TeamRepository teamRepository,
            BidRepository bidRepository,
            AuctionStateRepository auctionStateRepository,
            SimpMessagingTemplate messagingTemplate) {
        this.playerRepository = playerRepository;
        this.teamRepository = teamRepository;
        this.bidRepository = bidRepository;
        this.auctionStateRepository = auctionStateRepository;
        this.messagingTemplate = messagingTemplate;
    }

    // Cache for frequently accessed data
    private List<Player> playersCache;
    private List<Team> teamsCache;
    private long playersCacheTimestamp = 0;
    private long teamsCacheTimestamp = 0;
    private static final long CACHE_DURATION = 5000; // 5 seconds

    @Transactional
    public void resetAuctionState() {
        auctionStateRepository.deleteAll();
        List<Player> players = playerRepository.findAllByOrderById();
        if (!players.isEmpty()) {
            AuctionState state = new AuctionState();

            // Find first unsold player
            Player firstUnsoldPlayer = findNextUnsoldPlayer(players, 0);
            if (firstUnsoldPlayer != null) {
                state.setCurrentPlayer(firstUnsoldPlayer);
                state.setCurrentPlayerIndex(players.indexOf(firstUnsoldPlayer));

                // Find next unsold player after the current one
                Player nextUnsoldPlayer = findNextUnsoldPlayer(players, state.getCurrentPlayerIndex() + 1);
                state.setNextPlayer(nextUnsoldPlayer);

                auctionStateRepository.save(state);

                // Clear notifications
                notifications.clear();
                notifications.add("Auction reset and started with player: " + firstUnsoldPlayer.getName());
            } else {
                // All players are sold
                AuctionState emptyState = new AuctionState();
                emptyState.setCurrentPlayerIndex(players.size());
                auctionStateRepository.save(emptyState);
                notifications.clear();
                notifications.add("Auction reset, but all players are already sold.");
            }

            broadcastUpdate();
            System.out.println("Auction state reset with first available player");
        } else {
            System.out.println("No players found. Auction reset failed.");
        }
    }

    @Transactional(readOnly = true)
    public Team getTeamById(Long teamId) {
        return teamRepository.findById(teamId).orElse(null);
    }

    /**
     * Get the current bid amount for the current player
     */
    @Transactional(readOnly = true)
    public Integer getCurrentBidAmount() {
        AuctionState state = getCurrentState();
        if (state.getCurrentPlayer() == null) {
            return 0;
        }

        // If there's no bid yet, return base price only
        if (state.getHighestBid() == null) {
            return state.getCurrentPlayer().getBasePrice();
        }

        // Otherwise return the next bid amount
        return state.getCurrentPlayer().getNextBidAmount(state.getHighestBid().getAmount());
    }


    /**
     * Get all players with details for export
     */
    @Transactional(readOnly = true)
    public List<Player> getAllPlayersWithDetails() {
        return playerRepository.findAll();
    }

    /**
     * Get all players with details for export, but exclude basePrice and stepUpAmount
     */
    @Transactional(readOnly = true)
    public List<PlayerExportDTO> getAllPlayersForExport() {
        List<Player> players = playerRepository.findAll();
        return players.stream()
                .map(PlayerExportDTO::new)
                .collect(Collectors.toList());
    }

    private void broadcastUpdate() {
        AuctionState state = getCurrentState();
        List<Team> teams = getTeams();

        messagingTemplate.convertAndSend("/topic/auction", state);
        messagingTemplate.convertAndSend("/topic/teams", teams);
    }

    /**
     * Helper method to format currency amounts for better readability in notifications
     */
    private String formatCurrency(Integer amount) {
        if (amount == null) return "0";

        // Format as X,XX,XX,XXX for Indian number system
        String amountStr = amount.toString();
        StringBuilder formatted = new StringBuilder();

        int len = amountStr.length();
        for (int i = 0; i < len; i++) {
            formatted.append(amountStr.charAt(i));
            int remainingDigits = len - i - 1;

            if (remainingDigits > 0 && remainingDigits % 2 == 0 && remainingDigits != 2) {
                formatted.append(',');
            } else if (remainingDigits == 2) {
                formatted.append(',');
            }
        }

        return formatted.toString();
    }

    /**
     * Helper method to format currency amounts for better readability in notifications
     */
    private String formatCurrency(Long amount) {
        if (amount == null) return "0";

        // Format as X,XX,XX,XXX for Indian number system
        String amountStr = amount.toString();
        StringBuilder formatted = new StringBuilder();

        int len = amountStr.length();
        for (int i = 0; i < len; i++) {
            formatted.append(amountStr.charAt(i));
            int remainingDigits = len - i - 1;

            if (remainingDigits > 0 && remainingDigits % 2 == 0 && remainingDigits != 2) {
                formatted.append(',');
            } else if (remainingDigits == 2) {
                formatted.append(',');
            }
        }

        return formatted.toString();
    }

    /**
     * Edit the highest bid for the current player
     * This allows the auction administrator to modify the team and amount of the highest bid
     */
    @Transactional
    public Bid editHighestBid(Long teamId, Integer amount) {
        AuctionState state = getCurrentState();
        Team team = getTeamById(teamId);
        Player currentPlayer = state.getCurrentPlayer();

        if (team == null || currentPlayer == null || amount <= 0) {
            return null;
        }

        // Create a new bid with the edited information
        Bid bid = new Bid();
        bid.setTeamId(teamId);
        bid.setPlayerId(currentPlayer.getId());
        bid.setAmount(amount);
        bid.setTimestamp(LocalDateTime.now());

        // Save bid to database
        bid = bidRepository.save(bid);

        // Update auction state with new highest bid
        state.setHighestBid(bid);
        auctionStateRepository.save(state);

        // Format the bid amount for better readability
        String formattedAmount = formatCurrency(amount);

        // Add notification
        String notification = "Admin edited bid: " + team.getName() + " bid " + formattedAmount + " for " + currentPlayer.getName();
        notifications.add(notification);

        // Broadcast update
        broadcastUpdate();

        return bid;
    }

    /**
     * New method for re-auction functionality
     * This starts a new auction with only unsold players
     */
    @Transactional
    public Map<String, Object> startReAuction() {
        Map<String, Object> result = new HashMap<>();

        try {
            // Get all unsold players
            List<Player> allPlayers = playerRepository.findAllByOrderById();
            List<Player> unsoldPlayers = allPlayers.stream()
                    .filter(player -> !player.getSold())
                    .collect(Collectors.toList());

            if (unsoldPlayers.isEmpty()) {
                result.put("success", false);
                result.put("message", "No unsold players available for re-auction");
                return result;
            }

            // Clear existing auction state
            auctionStateRepository.deleteAll();

            // Create new auction state with first unsold player
            AuctionState state = new AuctionState();
            Player firstUnsoldPlayer = unsoldPlayers.get(0);

            state.setCurrentPlayer(firstUnsoldPlayer);
            state.setCurrentPlayerIndex(allPlayers.indexOf(firstUnsoldPlayer));

            // Find next unsold player for preview
            if (unsoldPlayers.size() > 1) {
                Player nextUnsoldPlayer = unsoldPlayers.get(1);
                state.setNextPlayer(nextUnsoldPlayer);
            } else {
                state.setNextPlayer(null);
            }

            auctionStateRepository.save(state);

            // Clear previous notifications and add re-auction notification
            notifications.clear();
            notifications.add("Re-auction started with " + unsoldPlayers.size() + " unsold players");
            notifications.add("Starting with player: " + firstUnsoldPlayer.getName());

            // Broadcast update to all clients
            broadcastUpdate();

            result.put("success", true);
            result.put("message", "Re-auction started successfully with " + unsoldPlayers.size() + " unsold players");
            result.put("unsoldPlayersCount", unsoldPlayers.size());
            result.put("currentPlayer", firstUnsoldPlayer.getName());

            System.out.println("Re-auction started with " + unsoldPlayers.size() + " unsold players");

        } catch (Exception e) {
            System.err.println("Error starting re-auction: " + e.getMessage());
            result.put("success", false);
            result.put("message", "Failed to start re-auction: " + e.getMessage());
        }

        return result;
    }

    /**
     * Update a specific player's details - ADMIN ONLY
     * This method allows updating critical fields like sold status, team assignment, etc.
     */
    @Transactional
    public Map<String, Object> updatePlayer(Long playerId, Player updatedPlayer) {
        Map<String, Object> result = new HashMap<>();

        try {
            Player existingPlayer = playerRepository.findById(playerId).orElse(null);
            if (existingPlayer == null) {
                result.put("success", false);
                result.put("message", "Player not found with ID: " + playerId);
                return result;
            }

            // Store original values for logging
            String originalName = existingPlayer.getName();
            Boolean originalSold = existingPlayer.getSold();
            Long originalTeamId = existingPlayer.getSoldToTeamId();
            Integer originalAmount = existingPlayer.getSoldAmount();

            // Handle team wallet adjustments if sold status or amount changes
            if (updatedPlayer.getSold() != null && updatedPlayer.getSoldToTeamId() != null) {
                // If player was previously sold to a team, refund that team
                if (originalSold && originalTeamId != null && originalAmount != null &&
                        (!updatedPlayer.getSold() || !originalTeamId.equals(updatedPlayer.getSoldToTeamId()) ||
                                !originalAmount.equals(updatedPlayer.getSoldAmount()))) {

                    Team originalTeam = getTeamById(originalTeamId);
                    if (originalTeam != null) {
                        originalTeam.setWalletBalance(originalTeam.getWalletBalance() + originalAmount);
                        teamRepository.save(originalTeam);
                    }
                }

                // If player is now sold to a team, deduct from that team's wallet
                if (updatedPlayer.getSold() && updatedPlayer.getSoldAmount() != null) {
                    Team newTeam = getTeamById(updatedPlayer.getSoldToTeamId());
                    if (newTeam == null) {
                        result.put("success", false);
                        result.put("message", "Invalid team ID: " + updatedPlayer.getSoldToTeamId());
                        return result;
                    }

                    // Check if team has sufficient balance
                    if (newTeam.getWalletBalance() < updatedPlayer.getSoldAmount()) {
                        result.put("success", false);
                        result.put("message", "Team " + newTeam.getName() + " has insufficient balance. Available: " +
                                formatCurrency(newTeam.getWalletBalance()) + ", Required: " +
                                formatCurrency(updatedPlayer.getSoldAmount()));
                        return result;
                    }

                    newTeam.setWalletBalance(newTeam.getWalletBalance() - updatedPlayer.getSoldAmount());
                    teamRepository.save(newTeam);
                }
            }

            // Update player fields
            if (updatedPlayer.getName() != null && !updatedPlayer.getName().trim().isEmpty()) {
                existingPlayer.setName(updatedPlayer.getName());
            }
            if (updatedPlayer.getPhotoUrl() != null) {
                existingPlayer.setPhotoUrl(updatedPlayer.getPhotoUrl());
            }
            if (updatedPlayer.getPlayerType() != null) {
                existingPlayer.setPlayerType(updatedPlayer.getPlayerType());
            }
            if (updatedPlayer.getTier() != null) {
                existingPlayer.setTier(updatedPlayer.getTier());
            }
            if (updatedPlayer.getLastYearTeam() != null) {
                existingPlayer.setLastYearTeam(updatedPlayer.getLastYearTeam());
            }
            if (updatedPlayer.getRuns() != null) {
                existingPlayer.setRuns(updatedPlayer.getRuns());
            }
            if (updatedPlayer.getBattingStat() != null) {
                existingPlayer.setBattingStat(updatedPlayer.getBattingStat());
            }
            if (updatedPlayer.getBowlingStat() != null) {
                existingPlayer.setBowlingStat(updatedPlayer.getBowlingStat());
            }
            if (updatedPlayer.getSold() != null) {
                existingPlayer.setSold(updatedPlayer.getSold());
            }
            if (updatedPlayer.getSoldToTeamId() != null) {
                existingPlayer.setSoldToTeamId(updatedPlayer.getSoldToTeamId());
            }
            if (updatedPlayer.getSoldAmount() != null) {
                existingPlayer.setSoldAmount(updatedPlayer.getSoldAmount());
            }

            // Save the updated player
            Player savedPlayer = playerRepository.save(existingPlayer);

            // Add notification about the update
            String teamName = "Unknown";
            if (savedPlayer.getSoldToTeamId() != null) {
                Team team = getTeamById(savedPlayer.getSoldToTeamId());
                if (team != null) {
                    teamName = team.getName();
                }
            }

            String notification = "Admin updated player: " + savedPlayer.getName() +
                    (savedPlayer.getSold() ?
                            " (Sold to " + teamName + " for " + formatCurrency(savedPlayer.getSoldAmount()) + ")" :
                            " (Unsold)");
            notifications.add(notification);

            // Broadcast update to all clients
            broadcastUpdate();

            result.put("success", true);
            result.put("message", "Player updated successfully");
            result.put("player", savedPlayer);

            System.out.println("Player updated: " + savedPlayer.getName() + " by admin");

        } catch (Exception e) {
            System.err.println("Error updating player: " + e.getMessage());
            result.put("success", false);
            result.put("message", "Failed to update player: " + e.getMessage());
        }

        return result;
    }

    // Optimized method to get cached players
    private List<Player> getCachedPlayers() {
        long currentTime = System.currentTimeMillis();
        if (playersCache == null || (currentTime - playersCacheTimestamp) > CACHE_DURATION) {
            playersCache = playerRepository.findAllByOrderById();
            playersCacheTimestamp = currentTime;
        }
        return playersCache;
    }

    // Optimized method to get cached teams
    private List<Team> getCachedTeams() {
        long currentTime = System.currentTimeMillis();
        if (teamsCache == null || (currentTime - teamsCacheTimestamp) > CACHE_DURATION) {
            teamsCache = teamRepository.findAll();
            teamsCacheTimestamp = currentTime;
        }
        return teamsCache;
    }

    /**
     * OPTIMIZED: Faster currency formatting using StringBuilder pre-allocation
     */
    private String formatCurrencyFast(Integer amount) {
        if (amount == null) return "0";

        String amountStr = amount.toString();
        int len = amountStr.length();

        // Pre-allocate StringBuilder with estimated capacity
        StringBuilder formatted = new StringBuilder(len + (len / 2));

        for (int i = 0; i < len; i++) {
            formatted.append(amountStr.charAt(i));
            int remainingDigits = len - i - 1;

            if (remainingDigits > 0 && remainingDigits % 2 == 0 && remainingDigits != 2) {
                formatted.append(',');
            } else if (remainingDigits == 2) {
                formatted.append(',');
            }
        }

        return formatted.toString();
    }

    /**
     * OPTIMIZED: Asynchronous broadcast to avoid blocking the bid operation
     */
    @Async
    public void broadcastUpdateAsync() {
        try {
            AuctionState state = getCurrentState();
            List<Team> teams = getCachedTeams();

            messagingTemplate.convertAndSend("/topic/auction", state);
            messagingTemplate.convertAndSend("/topic/teams", teams);
        } catch (Exception e) {
            System.err.println("Error broadcasting update: " + e.getMessage());
        }
    }

    /**
     * OPTIMIZED: Move to next player with cached data
     */
    @Transactional
    private void moveToNextPlayerOptimized() {
        AuctionState state = getCurrentState();
        List<Player> players = getCachedPlayers();

        // Reset bidding state
        state.setHighestBid(null);

        // Get the next unsold player after the current index
        int nextIndex = state.getCurrentPlayerIndex() + 1;
        Player nextUnsoldPlayer = findNextUnsoldPlayer(players, nextIndex);

        if (nextUnsoldPlayer != null) {
            state.setCurrentPlayer(nextUnsoldPlayer);
            state.setCurrentPlayerIndex(players.indexOf(nextUnsoldPlayer));

            // Find the next unsold player after this one for "next player" preview
            Player nextNextPlayer = findNextUnsoldPlayer(players, state.getCurrentPlayerIndex() + 1);
            state.setNextPlayer(nextNextPlayer);

            // Log skipped players if any
            if (players.indexOf(nextUnsoldPlayer) > nextIndex) {
                int skipped = players.indexOf(nextUnsoldPlayer) - nextIndex;
                String message = "Skipped " + skipped + " already sold player" + (skipped > 1 ? "s" : "");
                notifications.add(message);
            }
        } else {
            // Auction complete
            state.setCurrentPlayer(null);
            state.setNextPlayer(null);
            state.setCurrentPlayerIndex(players.size());
            notifications.add("Auction complete!");
        }

        auctionStateRepository.save(state);
    }

    /**
     * OPTIMIZED: Get current state with reduced queries
     */
    @Transactional(readOnly = true)
    public AuctionState getCurrentState() {
        AuctionState state = auctionStateRepository.findAll().stream()
                .findFirst()
                .orElse(null);

        if (state == null) {
            // Initialize state if missing
            List<Player> players = getCachedPlayers();
            if (!players.isEmpty()) {
                state = new AuctionState();
                Player firstUnsoldPlayer = findNextUnsoldPlayer(players, 0);
                if (firstUnsoldPlayer != null) {
                    state.setCurrentPlayer(firstUnsoldPlayer);
                    state.setCurrentPlayerIndex(players.indexOf(firstUnsoldPlayer));
                    Player nextUnsoldPlayer = findNextUnsoldPlayer(players, state.getCurrentPlayerIndex() + 1);
                    state.setNextPlayer(nextUnsoldPlayer);
                } else {
                    state.setCurrentPlayerIndex(players.size());
                }
                state = auctionStateRepository.save(state);
            } else {
                state = new AuctionState();
            }
        }

        // Add notifications
        state.setNotifications(notifications);

        // Calculate bid amounts for frontend
        if (state.getCurrentPlayer() != null) {
            state.setBasePrice(state.getCurrentPlayer().getBasePrice());
            state.setStepUpAmount(state.getCurrentPlayer().getStepUpAmount());

            if (state.getHighestBid() == null) {
                state.setNextBidAmount(state.getCurrentPlayer().getBasePrice());
            } else {
                state.setNextBidAmount(state.getCurrentPlayer().getNextBidAmount(state.getHighestBid().getAmount()));
            }
        }

        return state;
    }

    /**
     * OPTIMIZED: Get teams with caching
     */
    @Transactional(readOnly = true)
    public List<Team> getTeams() {
        return getCachedTeams();
    }

    // Keep existing helper methods unchanged
    private Player findNextUnsoldPlayer(List<Player> players, int startIndex) {
        for (int i = startIndex; i < players.size(); i++) {
            Player player = players.get(i);
            if (!player.getSold()) {
                return player;
            }
        }
        return null;
    }

    /**
     * FIXED: Sell current player with proper cache invalidation and immediate broadcast
     */
    @Transactional
    public Player sellCurrentPlayer() {
        AuctionState state = getCurrentState();

        if (state.getCurrentPlayer() == null || state.getHighestBid() == null) {
            return null;
        }

        Player player = state.getCurrentPlayer();
        Bid winningBid = state.getHighestBid();

        // Get team in single query
        Team winningTeam = teamRepository.findById(winningBid.getTeamId()).orElse(null);
        if (winningTeam == null) {
            return null;
        }

        // Update player and team in batch
        player.setSold(true);
        player.setSoldToTeamId(winningTeam.getId());
        player.setSoldAmount(winningBid.getAmount());

        winningTeam.setWalletBalance(winningTeam.getWalletBalance() - winningBid.getAmount());

        // Save both entities
        playerRepository.save(player);
        teamRepository.save(winningTeam);

        // CRITICAL: Invalidate cache BEFORE broadcasting
        invalidateCache();

        // Add notification
        String notification = player.getName() + " sold to " + winningTeam.getName() + " for " + formatCurrencyFast(winningBid.getAmount());
        notifications.add(notification);

        // Move to next player
        moveToNextPlayerOptimized();

        // CRITICAL: Broadcast with fresh data (not cached)
        broadcastUpdateImmediate();

        return player;
    }

    /**
     * FIXED: Mark player as unsold with proper cache invalidation
     */
    @Transactional
    public Player markCurrentPlayerUnsold() {
        AuctionState state = getCurrentState();

        if (state.getCurrentPlayer() == null) {
            return null;
        }

        Player player = state.getCurrentPlayer();
        player.setSold(false);
        playerRepository.save(player);

        // CRITICAL: Invalidate cache BEFORE broadcasting
        invalidateCache();

        // Add notification
        String notification = player.getName() + " went unsold";
        notifications.add(notification);

        // Move to next player
        moveToNextPlayer();

        // CRITICAL: Broadcast with fresh data
        broadcastUpdateImmediate();

        return player;
    }

    /**
     * FIXED: Place bid with proper cache handling
     */
    @Transactional
    public Bid placeBid(Long teamId) {
        // Get current state first
        AuctionState state = getCurrentState();
        if (state.getCurrentPlayer() == null) {
            return null;
        }

        Player currentPlayer = state.getCurrentPlayer();

        // Get team with single query instead of separate method call
        Team team = teamRepository.findById(teamId).orElse(null);
        if (team == null) {
            return null;
        }

        // Calculate bid amount (no DB calls needed here)
        Integer bidAmount;
        if (state.getHighestBid() == null) {
            bidAmount = currentPlayer.getBasePrice();
        } else {
            bidAmount = currentPlayer.getNextBidAmount(state.getHighestBid().getAmount());
        }

        // Check if team has enough balance
        if (team.getWalletBalance() < bidAmount) {
            return null;
        }

        // Create and save bid in one operation
        Bid bid = new Bid();
        bid.setTeamId(teamId);
        bid.setPlayerId(currentPlayer.getId());
        bid.setAmount(bidAmount);
        bid.setTimestamp(LocalDateTime.now());
        bid = bidRepository.save(bid);

        // Update auction state with new highest bid
        state.setHighestBid(bid);
        auctionStateRepository.save(state);

        // Add notification (optimized formatting)
        String notification = team.getName() + " bid " + formatCurrencyFast(bidAmount) + " for " + currentPlayer.getName();
        notifications.add(notification);

        // CRITICAL: Invalidate cache since team balance will change
        invalidateCache();

        // Broadcast update immediately with fresh data
        broadcastUpdateImmediate();

        return bid;
    }

    /**
     * NEW: Immediate broadcast with fresh data (not async, not cached)
     */
    private void broadcastUpdateImmediate() {
        try {
            AuctionState state = getCurrentState();
            // Get fresh teams data directly from database, not cache
            List<Team> teams = teamRepository.findAll();

            messagingTemplate.convertAndSend("/topic/auction", state);
            messagingTemplate.convertAndSend("/topic/teams", teams);

            System.out.println("Broadcasted update - Teams count: " + teams.size());

        } catch (Exception e) {
            System.err.println("Error broadcasting immediate update: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * FIXED: Move to next player with proper cache invalidation
     */
    @Transactional
    private void moveToNextPlayer() {
        AuctionState state = getCurrentState();
        List<Player> players = playerRepository.findAllByOrderById(); // Fresh data, not cached

        // Reset bidding state
        state.setHighestBid(null);

        // Get the next unsold player after the current index
        int nextIndex = state.getCurrentPlayerIndex() + 1;
        Player nextUnsoldPlayer = findNextUnsoldPlayer(players, nextIndex);

        if (nextUnsoldPlayer != null) {
            // Update current player and index
            state.setCurrentPlayer(nextUnsoldPlayer);
            state.setCurrentPlayerIndex(players.indexOf(nextUnsoldPlayer));

            // Find the next unsold player after this one for "next player" preview
            Player nextNextPlayer = findNextUnsoldPlayer(players, state.getCurrentPlayerIndex() + 1);
            state.setNextPlayer(nextNextPlayer);

            // Log and notification about skipped players
            if (players.indexOf(nextUnsoldPlayer) > nextIndex) {
                int skipped = players.indexOf(nextUnsoldPlayer) - nextIndex;
                String message = "Skipped " + skipped + " already sold player" + (skipped > 1 ? "s" : "");
                notifications.add(message);
                System.out.println(message);
            }
        } else {
            // Auction complete - no more unsold players
            state.setCurrentPlayer(null);
            state.setNextPlayer(null);
            state.setCurrentPlayerIndex(players.size());
            notifications.add("Auction complete!");
        }

        // Save the updated state
        auctionStateRepository.save(state);
        System.out.println("Moved to next player: " +
                (state.getCurrentPlayer() != null ? state.getCurrentPlayer().getName() : "Auction complete"));
    }

    /**
     * ENHANCED: Clear cache method
     */
    private void invalidateCache() {
        playersCache = null;
        teamsCache = null;
        playersCacheTimestamp = 0;
        teamsCacheTimestamp = 0;
        System.out.println("Cache invalidated");
    }
}