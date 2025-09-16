package com.projectmanager.repository;

import com.projectmanager.model.Project;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Repositório simples em memória para Project.
 * Implementação singleton — substitua por implementação persistente (DB) quando desejar.
 */
public class ProjectRepository {
    private static ProjectRepository instance;
    private final List<Project> projects = new ArrayList<>();

    private ProjectRepository() {}

    public static synchronized ProjectRepository getInstance() {
        if (instance == null) instance = new ProjectRepository();
        return instance;
    }

    public synchronized void save(Project project) {
        // substituir se já existir
        this.projects.removeIf(p -> p.getId().equals(project.getId()));
        this.projects.add(project);
    }

    public synchronized Optional<Project> findById(String id) {
        return projects.stream().filter(p -> p.getId().equals(id)).findFirst();
    }

    public synchronized List<Project> findAll() {
        return new ArrayList<>(projects);
    }

    public synchronized void delete(String id) {
        projects.removeIf(p -> p.getId().equals(id));
    }

    public synchronized List<Project> findByManagerId(String managerId) {
        List<Project> result = new ArrayList<>();
        for (Project p : projects) {
            if (p.getGerenteId() != null && p.getGerenteId().equals(managerId)) {
                result.add(p);
            }
        }
        return result;
    }
}
