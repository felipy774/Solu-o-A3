package com.projectmanager.repository;

import com.projectmanager.model.Team;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Repositório simples em memória para Team.
 */
public class TeamRepository {
    private static TeamRepository instance;
    private final List<Team> teams = new ArrayList<>();

    private TeamRepository() {}

    public static synchronized TeamRepository getInstance() {
        if (instance == null) instance = new TeamRepository();
        return instance;
    }

    public synchronized void save(Team team) {
        this.teams.removeIf(t -> t.getId().equals(team.getId()));
        this.teams.add(team);
    }

    public synchronized Optional<Team> findById(String id) {
        return teams.stream().filter(t -> t.getId().equals(id)).findFirst();
    }

    public synchronized List<Team> findAll() {
        return new ArrayList<>(teams);
    }

    public synchronized void delete(String id) {
        teams.removeIf(t -> t.getId().equals(id));
    }

    public synchronized List<Team> findByMemberId(String userId) {
        List<Team> result = new ArrayList<>();
        for (Team t : teams) {
            if (t.isMember(userId)) result.add(t);
        }
        return result;
    }
}
