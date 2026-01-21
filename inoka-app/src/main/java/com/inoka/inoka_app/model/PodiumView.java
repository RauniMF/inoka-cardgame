package com.inoka.inoka_app.model;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

public class PodiumView {
    private List<PodiumEntry> entries;

    public PodiumView() {
        this.entries = new ArrayList<>();
    }

    public PodiumView(List<PodiumEntry> entries) {
        this.entries = entries;
    }

    public List<PodiumEntry> getEntries() {
        return entries;
    }

    public void setEntries(List<PodiumEntry> entries) {
        this.entries = entries;
    }

    public static PodiumView fromGame(Game game) {
        List<Player> allPlayers = new ArrayList<>(game.getPlayers().values());
    
        // Check for Win Condition #2: Only one player has cards remaining
        int playersWithCards = 0;
        Optional<Player> onlyPlayerWithCardsOpt = Optional.empty();
        
        for (Player player : allPlayers) {
            if (player.getDeckSize() > 0) {
                playersWithCards++;
                if (onlyPlayerWithCardsOpt.isEmpty()) {
                    onlyPlayerWithCardsOpt = Optional.of(player);
                }
                else if (
                    onlyPlayerWithCardsOpt.isPresent() &&
                    !onlyPlayerWithCardsOpt.get().equals(null)
                ) {
                    onlyPlayerWithCardsOpt = Optional.ofNullable(null);
                }
            }
        }
        
        List<PodiumEntry> entries = new ArrayList<>();
        
        // Win Condition #2: Special handling for single player with cards
        if (
            playersWithCards == 1 &&
            onlyPlayerWithCardsOpt.isPresent() && 
            !onlyPlayerWithCardsOpt.get().equals(null)
        ) {
            // 1st place: The only player with cards
            List<PlayerView> firstPlaceList = new ArrayList<>();
            Player onlyPlayerWithCards = onlyPlayerWithCardsOpt.get();
            
            firstPlaceList.add(PlayerView.fromPlayer(onlyPlayerWithCards, 
                game.getSeatForPlayer(onlyPlayerWithCards.getId())));
            entries.add(new PodiumEntry(1, firstPlaceList, onlyPlayerWithCards.getSacredStones()));
            
            // Remaining players: Rank by sacred stones
            List<Player> remainingPlayers = new ArrayList<>();
            for (Player player : allPlayers) {
                if (!player.getId().equals(onlyPlayerWithCards.getId())) {
                    remainingPlayers.add(player);
                }
            }
            
            remainingPlayers.sort(Comparator.comparingInt(Player::getSacredStones).reversed());
            
            int currentPlacement = 2;
            int i = 0;
            
            while (i < remainingPlayers.size()) {
                int currentStoneCount = remainingPlayers.get(i).getSacredStones();
                List<PlayerView> tiedPlayers = new ArrayList<>();
                
                while (i < remainingPlayers.size() && 
                    remainingPlayers.get(i).getSacredStones() == currentStoneCount) {
                    Player player = remainingPlayers.get(i);
                    tiedPlayers.add(PlayerView.fromPlayer(player, game.getSeatForPlayer(player.getId())));
                    i++;
                }
                
                entries.add(new PodiumEntry(currentPlacement, tiedPlayers, currentStoneCount));
                currentPlacement = i + 2;
            }
        }
        // Win Conditions #1 & #3: Rank all players by sacred stones
        else {
            allPlayers.sort(Comparator.comparingInt(Player::getSacredStones).reversed());
            
            int currentPlacement = 1;
            int i = 0;
            
            while (i < allPlayers.size()) {
                int currentStoneCount = allPlayers.get(i).getSacredStones();
                List<PlayerView> tiedPlayers = new ArrayList<>();
                
                while (i < allPlayers.size() && 
                    allPlayers.get(i).getSacredStones() == currentStoneCount) {
                    Player player = allPlayers.get(i);
                    tiedPlayers.add(PlayerView.fromPlayer(player, game.getSeatForPlayer(player.getId())));
                    i++;
                }
                
                entries.add(new PodiumEntry(currentPlacement, tiedPlayers, currentStoneCount));
                currentPlacement = i + 1;
            }
        }
        
        return new PodiumView(entries);
    }


    public static class PodiumEntry {
        private int placement;
        private List<PlayerView> players;
        private int sacredStones;

        public PodiumEntry() {}

        public PodiumEntry(int placement, List<PlayerView> players, int sacredStones) {
            this.placement = placement;
            this.players = players;
            this.sacredStones = sacredStones;
        }

        public int getPlacement() {
            return placement;
        }

        public void setPlacement(int placement) {
            this.placement = placement;
        }

        public List<PlayerView> getPlayers() {
            return players;
        }

        public void setPlayers(List<PlayerView> players) {
            this.players = players;
        }

        public int getSacredStones() {
            return sacredStones;
        }

        public void setSacredStones(int sacredStones) {
            this.sacredStones = sacredStones;
        }
    }
}