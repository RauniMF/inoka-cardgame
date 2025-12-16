package com.inoka.inoka_app.service;

import java.util.Optional;

import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import com.inoka.inoka_app.model.Player;
import com.inoka.inoka_app.repositories.PlayerRepository;
import com.inoka.inoka_app.security.PlayerPrincipal;

@Service
public class CustomUserDetailsService implements UserDetailsService {
    private PlayerRepository playerRepository;

    public CustomUserDetailsService(PlayerRepository playerRepository) {
        this.playerRepository = playerRepository;
    }

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        // treat 'username' as player UUID
        return loadUserById(username);
    }

    public PlayerPrincipal loadUserById(String userId) {
        Optional<Player> playerCheck = playerRepository.findById(userId);
        if (playerCheck.isEmpty()) {
            throw new UsernameNotFoundException("Player not found with id: " + userId);
        }
        return new PlayerPrincipal(playerCheck.get());
    }
}
