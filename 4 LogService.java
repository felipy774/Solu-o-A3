package com.projectmanager.service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Serviço simples de logs/histórico. Armazena entradas de log em memória.
 * Cada entrada contém: quem fez, quando, tipo da ação, entidade alvo e detalhes.
 */
public class LogService {
    public static class LogEntry {
        private final String userId;
        private final LocalDateTime timestamp;
        private final String action;
        private final String entity;
        private final String details;

        public LogEntry(String userId, LocalDateTime timestamp, String action, String entity, String details) {
            this.userId = userId;
            this.timestamp = timestamp;
            this.action = action;
            this.entity = entity;
            this.details = details;
        }

        public String getUserId() { return userId; }
        public LocalDateTime getTimestamp() { return timestamp; }
        public String getAction() { return action; }
        public String getEntity() { return entity; }
        public String getDetails() { return details; }

        @Override
        public String toString() {
            return String.format("[%s] user=%s action=%s entity=%s details=%s",
                    timestamp.toString(), userId, action, entity, details);
        }
    }

    private static LogService instance;
    private final List<LogEntry> entries = new ArrayList<>();

    private LogService() {}

    public static synchronized LogService getInstance() {
        if (instance == null) instance = new LogService();
        return instance;
    }

    public synchronized void log(String userId, String action, String entity, String details) {
        LogEntry entry = new LogEntry(userId, LocalDateTime.now(), action, entity, details);
        entries.add(entry);
    }

    public synchronized List<LogEntry> getEntries() {
        return new ArrayList<>(entries);
    }

    public synchronized List<LogEntry> getEntriesForEntity(String entityId) {
        List<LogEntry> result = new ArrayList<>();
        for (LogEntry e : entries) {
            if (e.getEntity().contains(entityId)) result.add(e);
        }
        return result;
    }
}
