package com.auction.cnpl.controller;

import com.auction.cnpl.dto.PlayerExportDTO;
import com.auction.cnpl.model.*;
import com.auction.cnpl.service.AuctionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api")
public class AuctionController {
    private final AuctionService auctionService;

    @Value("${allowed.ip:127.0.0.1}")
    private String allowedIp;

    @Autowired
    public AuctionController(AuctionService auctionService) {
        this.auctionService = auctionService;
    }

    @GetMapping("/auction/current")
    public ResponseEntity<AuctionState> getCurrentState() {
        return ResponseEntity.ok(auctionService.getCurrentState());
    }

    @GetMapping("/teams")
    public ResponseEntity<List<Team>> getTeams() {
        return ResponseEntity.ok(auctionService.getTeams());
    }

    /**
     * Get current bid amount for the frontend
     */
    @GetMapping("/auction/current-bid-amount")
    public ResponseEntity<Map<String, Object>> getCurrentBidAmount() {
        Integer amount = auctionService.getCurrentBidAmount();
        Map<String, Object> response = new HashMap<>();
        response.put("amount", amount);

        // Add player tier information for the frontend
        AuctionState state = auctionService.getCurrentState();
        if (state.getCurrentPlayer() != null) {
            response.put("basePrice", state.getBasePrice());
            response.put("stepUpAmount", state.getStepUpAmount());
            response.put("tier", state.getCurrentPlayer().getTier());
        }

        return ResponseEntity.ok(response);
    }

    /**
     * Modified to not require an amount parameter - it's automatically calculated
     */
    @PostMapping("/auction/bid")
    public ResponseEntity<Bid> placeBid(@RequestBody Map<String, Object> bidInfo) {
        Long teamId = Long.valueOf(bidInfo.get("teamId").toString());

        // We don't use the amount from the request anymore
        Bid bid = auctionService.placeBid(teamId);
        if (bid == null) {
            return ResponseEntity.badRequest().build();
        }
        return ResponseEntity.ok(bid);
    }

    @PostMapping("/auction/sold")
    public ResponseEntity<Player> sellPlayer() {
        Player player = auctionService.sellCurrentPlayer();
        if (player == null) {
            return ResponseEntity.badRequest().build();
        }
        return ResponseEntity.ok(player);
    }

    @PostMapping("/auction/unsold")
    public ResponseEntity<Player> markUnsold() {
        Player player = auctionService.markCurrentPlayerUnsold();
        if (player == null) {
            return ResponseEntity.badRequest().build();
        }
        return ResponseEntity.ok(player);
    }

    /**
     * Endpoint to check if client IP is allowed to access restricted endpoints
     */
    @GetMapping("/check-ip-access")
    public Map<String, Boolean> checkIpAccess(HttpServletRequest request) {
        String clientIp = getClientIp(request);
        System.out.println(clientIp);
        System.out.println(allowedIp);
        List<String> allowedIps = Arrays.asList(allowedIp.split(","));

        Map<String, Boolean> response = new HashMap<>();
        response.put("allowed", allowedIps.contains(clientIp));

        return response;
    }

    @PostMapping("/auction/edit-bid")
    public ResponseEntity<Bid> editHighestBid(@RequestBody Map<String, Object> editInfo, HttpServletRequest request) {
        // Security check: Only allowed IPs can edit bids
        String clientIp = getClientIp(request);
        List<String> allowedIps = Arrays.asList(allowedIp.split(","));
        if (!allowedIps.contains(clientIp)) {
            return ResponseEntity.status(403).build();
        }

        Long teamId = Long.valueOf(editInfo.get("teamId").toString());
        Integer amount = Integer.valueOf(editInfo.get("amount").toString());

        Bid bid = auctionService.editHighestBid(teamId, amount);
        if (bid == null) {
            return ResponseEntity.badRequest().build();
        }
        return ResponseEntity.ok(bid);
    }

    /**
     * New endpoint for re-auction functionality
     * This will start a new auction with only unsold players
     */
    @PostMapping("/auction/re-auction")
    public ResponseEntity<Map<String, Object>> startReAuction(HttpServletRequest request) {
        // Security check: Only allowed IPs can start re-auction
        String clientIp = getClientIp(request);
        List<String> allowedIPs = Arrays.asList(allowedIp.split(","));
        if (!allowedIPs.contains(clientIp)) {
            return ResponseEntity.status(403).build();
        }

        Map<String, Object> result = auctionService.startReAuction();
        if (result.get("success").equals(false)) {
            return ResponseEntity.badRequest().body(result);
        }
        return ResponseEntity.ok(result);
    }

    /**
     * Helper method to extract client IP from various headers
     */
    private String getClientIp(HttpServletRequest request) {
        String ip = request.getHeader("X-Forwarded-For");

        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("Proxy-Client-IP");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("WL-Proxy-Client-IP");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("HTTP_CLIENT_IP");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("HTTP_X_FORWARDED_FOR");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getRemoteAddr();
        }

        // If IP contains multiple addresses, take the first one
        if (ip != null && ip.contains(",")) {
            ip = ip.split(",")[0].trim();
        }

        return ip;
    }

    /**
     * Endpoint to get all players with their sold status, sold amount, and team
     */
    @GetMapping("/auction/export-players")
    public ResponseEntity<List<PlayerExportDTO>> getAllPlayersForExport() {
        List<PlayerExportDTO> players = auctionService.getAllPlayersForExport();
        return ResponseEntity.ok(players);
    }

    /**
     * Get all players with full details for management
     */
    @GetMapping("/admin/players")
    public ResponseEntity<List<Player>> getAllPlayersForManagement(HttpServletRequest request) {
        // Security check: Only allowed IPs can access player management
        String clientIp = getClientIp(request);
        List<String> allowedIps = Arrays.asList(allowedIp.split(","));
        if (!allowedIps.contains(clientIp)) {
            return ResponseEntity.status(403).build();
        }

        List<Player> players = auctionService.getAllPlayersWithDetails();
        return ResponseEntity.ok(players);
    }

    /**
     * Update a specific player's details
     */
    @PutMapping("/admin/players/{playerId}")
    public ResponseEntity<Map<String, Object>> updatePlayer(
            @PathVariable Long playerId,
            @RequestBody Player updatedPlayer,
            HttpServletRequest request) {

        // Security check: Only allowed IPs can update players
        String clientIp = getClientIp(request);
        List<String> allowedIps = Arrays.asList(allowedIp.split(","));
        if (!allowedIps.contains(clientIp)) {
            return ResponseEntity.status(403).build();
        }

        Map<String, Object> result = auctionService.updatePlayer(playerId, updatedPlayer);

        if (result.get("success").equals(false)) {
            return ResponseEntity.badRequest().body(result);
        }

        return ResponseEntity.ok(result);
    }
}