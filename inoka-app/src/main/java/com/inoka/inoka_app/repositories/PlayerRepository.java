package com.inoka.inoka_app.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.inoka.inoka_app.model.Player;

import java.util.Optional;

@Repository
public interface PlayerRepository extends JpaRepository<Player, String>{
    void deleteById(String id);
    Optional<Player> findPlayerById(String id);
}
