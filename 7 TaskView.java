package com.projectmanager.view;

import com.projectmanager.model.Project;
import com.projectmanager.model.ProjectStatus;
import com.projectmanager.model.Task;
import com.projectmanager.model.TaskStatus;
import com.projectmanager.repository.ProjectRepository;
import com.projectmanager.repository.TaskRepository;
import com.projectmanager.repository.TeamRepository;
import com.projectmanager.repository.UserRepository;
import com.projectmanager.service.AuthenticationService;
import com.projectmanager.service.LogService;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * View de console para gerenciar tarefas.
 * - valida campos obrigatórios antes de permitir avanço
 * - bloqueia ação se projeto estiver cancelado
 * - grava histórico de ações via LogService
 */
public class TaskView {
    private final TaskRepository taskRepo;
    private final ProjectRepository projectRepo;
    private final TeamRepository teamRepo;
    private final UserRepository userRepo;
    private final AuthenticationService authService;
    private final LogService logService;

    public TaskView() {
        this.taskRepo = TaskRepository.getInstance();
        this.projectRepo = ProjectRepository.getInstance();
        this.teamRepo = TeamRepository.getInstance();
        this.userRepo = UserRepository.getInstance();
        this.authService = AuthenticationService.getInstance();
        this.logService = LogService.getInstance();
    }

    public void mostrarMenu() {
        boolean voltar = false;
        while (!voltar) {
            ConsoleUtils.limparTela();
            ConsoleUtils.mostrarTitulo("GERENCIAMENTO DE TAREFAS");
            System.out.println("1. ➕ Criar Tarefa");
            System.out.println("2. 📋 Listar Tarefas");
            System.out.println("3. 🔍 Ver Tarefa");
            System.out.println("4. ▶️  Iniciar Tarefa");
            System.out.println("5. ✅ Concluir Tarefa");
            System.out.println("6. ✏️  Editar Tarefa");
            System.out.println("0. ⬅️ Voltar");
            int op = ConsoleUtils.lerInt("Escolha uma opção: ");
            switch (op) {
                case 1: criarTarefa(); break;
                case 2: listarTarefas(); break;
                case 3: verTarefa(); break;
                case 4: iniciarTarefa(); break;
                case 5: concluirTarefa(); break;
                case 6: editarTarefa(); break;
                case 0: voltar = true; break;
                default: ConsoleUtils.mostrarMensagemErro("Opção inválida!");
            }
            if (!voltar) ConsoleUtils.pausar();
        }
    }

    private void criarTarefa() {
        if (!authService.hasPermission("CREATE_TASK")) {
            ConsoleUtils.mostrarMensagemErro("Sem permissão para criar tarefa!");
            return;
        }
        String titulo = ConsoleUtils.lerString("Título: ");
        String descricao = ConsoleUtils.lerString("Descrição: ");
        String projectId = ConsoleUtils.lerString("ID do projeto: ");
        Optional<Project> optP = projectRepo.findById(projectId);
        if (optP.isEmpty()) { ConsoleUtils.mostrarMensagemErro("Projeto não encontrado!"); return; }
        Project project = optP.get();
        if (project.isCanceled()) { ConsoleUtils.mostrarMensagemErro("Projeto cancelado — não é permitido criar tarefas."); return; }

        String teamId = ConsoleUtils.lerString("ID da equipe responsável: ");
        if (teamRepo.findById(teamId).isEmpty()) { ConsoleUtils.mostrarMensagemErro("Equipe não encontrada!"); return; }

        Task task = new Task(titulo, descricao, projectId, teamId);
        task.validateRequiredFields();
        if (!task.isCamposObrigatoriosPreenchidos()) {
            ConsoleUtils.mostrarMensagemErro("Campos obrigatórios não preenchidos — tarefa criada em PENDENTE.");
            // permitimos criar, mas não avançar até preencher
        }
        taskRepo.save(task);

        // vincular tarefa ao projeto
        project.addTask(task.getId());
        projectRepo.save(project);

        logService.log(authService.getCurrentUser().getId(), "CREATE_TASK", task.getId(), 
                "Tarefa criada em projeto=" + projectId + " equipe=" + teamId);

        ConsoleUtils.mostrarMensagemSucesso("Tarefa criada com ID: " + task.getId());
    }

    private void listarTarefas() {
        ConsoleUtils.mostrarTitulo("TAREFAS CADASTRADAS");
        List<Task> tasks = taskRepo.findAll();
        if (tasks.isEmpty()) {
            System.out.println("Nenhuma tarefa cadastrada.");
            return;
        }
        for (Task t : tasks) {
            System.out.println(t.toString());
        }
    }

    private void verTarefa() {
        String id = ConsoleUtils.lerString("ID da tarefa: ");
        Optional<Task> opt = taskRepo.findById(id);
        if (opt.isEmpty()) { ConsoleUtils.mostrarMensagemErro("Tarefa não encontrada!"); return; }
        Task t = opt.get();
        ConsoleUtils.mostrarTitulo("TAREFA: " + t.getTitulo());
        System.out.println("ID: " + t.getId());
        System.out.println("Descrição: " + t.getDescricao());
        System.out.println("Projeto: " + t.getProjectId());
        System.out.println("Equipe: " + t.getTeamId());
        System.out.println("Responsável: " + (t.getResponsavelId() == null ? "Não atribuído" : t.getResponsavelId()));
        System.out.println("Status: " + t.getStatus().getDisplayName());
        System.out.println("Criada em: " + t.getDataCriacao());
        System.out.println("Vencimento: " + t.getDataVencimento());
        System.out.println("Conclusão: " + t.getDataConclusao());

        var logs = logService.getEntriesForEntity(t.getId());
        if (!logs.isEmpty()) {
            ConsoleUtils.mostrarSeparador();
            System.out.println("Histórico:");
            for (var l : logs) System.out.println(l.toString());
        }
    }

    private void iniciarTarefa() {
        String id = ConsoleUtils.lerString("ID da tarefa para iniciar: ");
        Optional<Task> opt = taskRepo.findById(id);
        if (opt.isEmpty()) { ConsoleUtils.mostrarMensagemErro("Tarefa não encontrada!"); return; }
        Task t = opt.get();

        // Verificar projeto associado
        Optional<Project> optP = projectRepo.findById(t.getProjectId());
        if (optP.isEmpty()) { ConsoleUtils.mostrarMensagemErro("Projeto não encontrado!"); return; }
        Project project = optP.get();
        if (project.isCanceled()) { ConsoleUtils.mostrarMensagemErro("Projeto cancelado — ação proibida."); return; }

        t.validateRequiredFields();
        if (!t.isCamposObrigatoriosPreenchidos()) {
            ConsoleUtils.mostrarMensagemErro("Campos obrigatórios não preenchidos — não é possível iniciar.");
            return;
        }

        String userId = ConsoleUtils.lerString("Seu ID (responsável): ");
        if (!authService.getCurrentUser().getId().equals(userId) && !authService.hasPermission("MANAGE_TASKS")) {
            ConsoleUtils.mostrarMensagemErro("Você só pode iniciar tarefas como você mesmo ou possuir permissão de gestão.");
            return;
        }

        boolean ok = t.markAsStarted(userId);
        if (ok) {
            taskRepo.save(t);
            logService.log(userId, "START_TASK", t.getId(), "Iniciou tarefa");
            ConsoleUtils.mostrarMensagemSucesso("Tarefa iniciada.");
        } else {
            ConsoleUtils.mostrarMensagemErro("Não foi possível iniciar a tarefa (status atual: " + t.getStatus().name() + ")");
        }
    }

    private void concluirTarefa() {
        String id = ConsoleUtils.lerString("ID da tarefa para concluir: ");
        Optional<Task> opt = taskRepo.findById(id);
        if (opt.isEmpty()) { ConsoleUtils.mostrarMensagemErro("Tarefa não encontrada!"); return; }
        Task t = opt.get();

        // Verificar projeto associado
        Optional<Project> optP = projectRepo.findById(t.getProjectId());
        if (optP.isEmpty()) { ConsoleUtils.mostrarMensagemErro("Projeto não encontrado!"); return; }
        Project project = optP.get();
        if (project.isCanceled()) { ConsoleUtils.mostrarMensagemErro("Projeto cancelado — ação proibida."); return; }

        String userId = ConsoleUtils.lerString("Seu ID (responsável): ");
        if (!userId.equals(t.getResponsavelId())) {
            ConsoleUtils.mostrarMensagemErro("Apenas o responsável pela tarefa pode concluí-la!");
            return;
        }

        boolean ok = t.markAsCompleted(userId);
        if (ok) {
            taskRepo.save(t);
            logService.log(userId, "COMPLETE_TASK", t.getId(), "Concluiu tarefa");
            ConsoleUtils.mostrarMensagemSucesso("Tarefa concluída.");
        } else {
            ConsoleUtils.mostrarMensagemErro("Não foi possível concluir a tarefa (status atual: " + t.getStatus().name() + ")");
        }
    }

    private void editarTarefa() {
        String id = ConsoleUtils.lerString("ID da tarefa para editar: ");
        Optional<Task> opt = taskRepo.findById(id);
        if (opt.isEmpty()) { ConsoleUtils.mostrarMensagemErro("Tarefa não encontrada!"); return; }
        Task t = opt.get();

        Optional<Project> optP = projectRepo.findById(t.getProjectId());
        if (optP.isEmpty()) { ConsoleUtils.mostrarMensagemErro("Projeto não encontrado!"); return; }
        Project project = optP.get();
        if (project.isCanceled()) { ConsoleUtils.mostrarMensagemErro("Projeto cancelado — não é possível editar."); return; }

        // Permite edição se for responsável ou tiver permissão
        boolean isResponsavel = authService.isLoggedIn() && authService.getCurrentUser().getId().equals(t.getResponsavelId());
        if (!isResponsavel && !authService.hasPermission("MANAGE_TASKS")) {
            ConsoleUtils.mostrarMensagemErro("Sem permissão para editar essa tarefa!");
            return;
        }

        String novoTitulo = ConsoleUtils.lerString("Novo título (enter para manter): ");
        String novaDesc = ConsoleUtils.lerString("Nova descrição (enter para manter): ");
        String novaVenc = ConsoleUtils.lerString("Nova data vencimento (yyyy-MM-ddTHH:mm) ou vazio: ");

        if (!novoTitulo.isEmpty()) t.setTitulo(novoTitulo);
        if (!novaDesc.isEmpty()) t.setDescricao(novaDesc);
        if (!novaVenc.isEmpty()) t.setDataVencimento(LocalDateTime.parse(novaVenc));

        t.validateRequiredFields();
        taskRepo.save(t);
        logService.log(authService.getCurrentUser().getId(), "EDIT_TASK", t.getId(), "Tarefa editada");
        ConsoleUtils.mostrarMensagemSucesso("Tarefa atualizada.");
    }
}
