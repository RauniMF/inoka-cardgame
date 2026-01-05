package com.inoka.inoka_app.security;

import java.util.Collection;
import java.util.Collections;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import com.inoka.inoka_app.model.Player;

public class PlayerPrincipal implements UserDetails {
    private final Player player;

    public PlayerPrincipal(Player player) {
        this.player = player;
    }

    public Player getPlayer() {
        return this.player;
    }

    public String getUserId() {
        return this.player.getId();
    }

    @Override
    public String getUsername() {
        // User UUID treated as principal subject
        return this.getUserId();
    }

    @Override
    public String getPassword() {
        // Players are treated as guests, returns empty
        return "";
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return Collections.singletonList(new SimpleGrantedAuthority("ROLE_USER"));
    }
}
