package com.inoka.inoka_app.service;

import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Service;

import com.inoka.inoka_app.model.Player;
import com.inoka.inoka_app.repositories.PlayerRepository;

@Service
public class PlayerService {
    // Repo containing player data (name, id, gameid)
    private final PlayerRepository playerRepository;

    public PlayerService(PlayerRepository playerRepository) {
        this.playerRepository = playerRepository;
    }

    public Player addPlayer(Player player) {
        return playerRepository.save(player);
    }

    public List<Player> findAllPlayers() {
        return playerRepository.findAll();
    }

    public Player savePlayer(Player player) {
        return playerRepository.save(player);
    }

    public boolean updatePlayer(String id, String name) {
        Optional<Player> playerCheck = playerRepository.findPlayerById(id);
        if (playerCheck.isPresent()) {
            Player player = playerCheck.get();
            player.setName(name);
            playerRepository.save(player);
            return true;
        } else {
            return false;
        }
    }

    public boolean removePlayerById(String id) {
        if (playerRepository.existsById(id)) {
            playerRepository.deleteById(id);
            return true;
        }
        else {
            return false;
        }
    }

    public Optional<Player> findPlayerById(String id) {
       return playerRepository.findPlayerById(id);
        
    }

    public void removeAllPlayers() {
        playerRepository.deleteAll();
    }
}
