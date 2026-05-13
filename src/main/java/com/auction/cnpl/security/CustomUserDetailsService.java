package com.auction.cnpl.security;

import com.auction.cnpl.model.TeamOwner;
import com.auction.cnpl.repository.TeamOwnerRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;

@Service
public class CustomUserDetailsService implements UserDetailsService {

    @Autowired
    private TeamOwnerRepository teamOwnerRepository;

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        TeamOwner teamOwner = teamOwnerRepository.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException("User not found: " + username));

        // Determine roles - for simplicity, let's say "admin" username has ADMIN role
        List<SimpleGrantedAuthority> authorities;
        if ("admin".equals(username)) {
            authorities = Collections.singletonList(new SimpleGrantedAuthority("ROLE_ADMIN"));
        } else {
            authorities = Collections.singletonList(new SimpleGrantedAuthority("ROLE_USER"));
        }

        return new User(teamOwner.getUsername(), teamOwner.getPassword(), authorities);
    }
}