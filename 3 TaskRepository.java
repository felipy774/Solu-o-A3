package com.projectmanager.repository;

import com.projectmanager.model.Task;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Repositório simples em memória para Task.
 */
public class TaskRepository {
    private static TaskRepository instance;
    private final List<Task> tasks = new ArrayList<>();

    private TaskRepository() {}

    public static synchronized TaskRepository getInstance() {
        if (instance == null) instance = new TaskRepository();
        return instance;
    }

    public synchronized void save(Task task) {
        this.tasks.removeIf(t -> t.getId().equals(task.getId()));
        this.tasks.add(task);
    }

    public synchronized Optional<Task> findById(String id) {
        return tasks.stream().filter(t -> t.getId().equals(id)).findFirst();
    }

    public synchronized List<Task> findAll() {
        return new ArrayList<>(tasks);
    }

    public synchronized List<Task> findByProjectId(String projectId) {
        List<Task> result = new ArrayList<>();
        for (Task t : tasks) {
            if (projectId.equals(t.getProjectId())) result.add(t);
        }
        return result;
    }

    public synchronized List<Task> findByTeamId(String teamId) {
        List<Task> result = new ArrayList<>();
        for (Task t : tasks) {
            if (teamId.equals(t.getTeamId())) result.add(t);
        }
        return result;
    }

    public synchronized void delete(String id) {
        tasks.removeIf(t -> t.getId().equals(id));
    }
}
