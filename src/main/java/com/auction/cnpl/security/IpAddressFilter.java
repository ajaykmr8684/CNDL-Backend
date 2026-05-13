package com.auction.cnpl.security;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

@Component
public class IpAddressFilter extends OncePerRequestFilter {

    private static final Logger logger = LoggerFactory.getLogger(IpAddressFilter.class);

    @Value("${allowed.ip:}")
    private String allowedIp;

    private List<String> getAllowedIps() {
        if (!StringUtils.hasText(allowedIp)) {
            logger.warn("No allowed IP addresses configured for restricted endpoints");
            return Collections.emptyList();
        }
        return Arrays.asList(allowedIp.split(","));
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        String path = request.getRequestURI();

        // Only apply IP restriction to bid and sold endpoints
        if (path.contains("/api/auction/bid") || path.contains("/api/auction/sold")) {
            // Wildcard disables IP filtering entirely
            if ("*".equals(allowedIp.trim())) {
                filterChain.doFilter(request, response);
                return;
            }

            List<String> allowedIps = getAllowedIps();

            // If no allowed IPs are configured, deny access to protected endpoints
            if (allowedIps.isEmpty()) {
                logger.error("Access attempted to protected endpoint without any allowed IPs configured");
                response.sendError(HttpServletResponse.SC_FORBIDDEN,
                        "Access denied: IP filtering is not properly configured");
                return;
            }

            String clientIp = getClientIp(request);
            logger.info("Client IP: {}, Allowed IPs: {}", clientIp, allowedIps);

            if (!allowedIps.contains(clientIp)) {
                logger.warn("Access denied for IP: {} attempting to access: {}", clientIp, path);
                response.sendError(HttpServletResponse.SC_FORBIDDEN,
                        "Access denied: Your IP is not allowed to access this resource");
                return;
            }

            logger.debug("Access granted for IP: {} to access: {}", clientIp, path);
        }

        filterChain.doFilter(request, response);
    }

    private String getClientIp(HttpServletRequest request) {
        String ip = request.getHeader("X-Forwarded-For");

        if (!StringUtils.hasText(ip) || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("Proxy-Client-IP");
        }
        if (!StringUtils.hasText(ip) || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("WL-Proxy-Client-IP");
        }
        if (!StringUtils.hasText(ip) || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("HTTP_CLIENT_IP");
        }
        if (!StringUtils.hasText(ip) || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("HTTP_X_FORWARDED_FOR");
        }
        if (!StringUtils.hasText(ip) || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getRemoteAddr();
        }

        // If IP contains multiple addresses, take the first one
        if (ip != null && ip.contains(",")) {
            ip = ip.split(",")[0].trim();
        }

        return ip;
    }
}
