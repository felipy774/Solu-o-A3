package com.projectmanager.view;

import com.projectmanager.model.Project;
import com.projectmanager.model.ProjectStatus;
import com.projectmanager.repository.ProjectRepository;
import com.projectmanager.repository.TeamRepository;
import com.projectmanager.service.AuthenticationService;
import com.projectmanager.service.LogService;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/**
 * View de console para gerenciar projetos.
 */
public class ProjectView {
    private final ProjectRepository projectRepo;
    private final TeamRepository teamRepo;
    private final AuthenticationService authService;
    private final LogService logService;

    public ProjectView() {
        this.projectRepo = ProjectRepository.getInstance();
        this.teamRepo = TeamRepository.getInstance();
        this.authService = AuthenticationService.getInstance();
        this.logService = LogService.getInstance();
    }

    public void mostrarMenu() {
        boolean voltar = false;
        while (!voltar) {
            ConsoleUtils.limparTela();
            ConsoleUtils.mostrarTitulo("GERENCIAMENTO DE PROJETOS");
            System.out.println("1. ➕ Criar Projeto");
            System.out.println("2. 📋 Listar Projetos");
            System.out.println("3. 🔍 Visualizar Projeto");
            System.out.println("4. ✏️  Editar Projeto");
            System.out.println("5. ❌ Cancelar Projeto");
            System.out.println("6. ♻️  Reativar Projeto");
            System.out.println("0. ⬅️ Voltar");
            
            int op = ConsoleUtils.lerInt("Escolha uma opção: ");
            switch (op) {
                case 1: criarProjeto(); break;
                case 2: listarProjetos(); break;
                case 3: visualizarProjeto(); break;
                case 4: editarProjeto(); break;
                case 5: cancelarProjeto(); break;
                case 6: reativarProjeto(); break;
                case 0: voltar = true; break;
                default: ConsoleUtils.mostrarMensagemErro("Opção inválida!");
            }
            if (!voltar) {
                ConsoleUtils.pausar();
            }
        }
    }

    private void criarProjeto() {
        ConsoleUtils.mostrarTitulo("CRIAR PROJETO");
        if (!authService.hasPermission("CREATE_PROJECT")) {
            ConsoleUtils.mostrarMensagemErro("Sem permissão para criar projeto!");
            return;
        }

        try {
            String nome = ConsoleUtils.lerString("Nome do projeto: ");
            String descricao = ConsoleUtils.lerString("Descrição: ");
            String dataInicioStr = ConsoleUtils.lerString("Data início (yyyy-MM-dd) [opcional]: ");
            String dataTerminoPrevistaStr = ConsoleUtils.lerString("Data término prevista (yyyy-MM-dd) [opcional]: ");

            LocalDate dataInicio = dataInicioStr.isEmpty() ? null : LocalDate.parse(dataInicioStr);
            LocalDate dataTerminoPrevista = dataTerminoPrevistaStr.isEmpty() ? null : LocalDate.parse(dataTerminoPrevistaStr);

            // escolha de gerente
            String gerenteId = ConsoleUtils.lerString("ID do gerente responsável: ");
            if (gerenteId.isEmpty()) {
                ConsoleUtils.mostrarMensagemErro("Gerente é obrigatório!");
                return;
            }

            Project project = new Project(nome, descricao, dataInicio, dataTerminoPrevista, gerenteId);
            project.setStatus(ProjectStatus.PLANEJADO);
            projectRepo.save(project);

            logService.log(authService.isLoggedIn() ? authService.getCurrentUser().getId() : "SYSTEM",
                    "CREATE_PROJECT", project.getId(),
                    "Projeto criado: " + nome + " gerente=" + gerenteId);

            ConsoleUtils.mostrarMensagemSucesso("Projeto criado com sucesso! ID: " + project.getId());
        } catch (Exception e) {
            ConsoleUtils.mostrarMensagemErro("Erro ao criar projeto: " + e.getMessage());
        }
    }

    private void listarProjetos() {
        ConsoleUtils.mostrarTitulo("LISTA DE PROJETOS");
        List<Project> projetos = projectRepo.findAll();
        if (projetos.isEmpty()) {
            System.out.println("Nenhum projeto cadastrado.");
            return;
        }
        for (Project p : projetos) {
            System.out.println(p.toString());
        }
    }

    private void visualizarProjeto() {
        String id = ConsoleUtils.lerString("ID do projeto: ");
        Optional<Project> opt = projectRepo.findById(id);
        if (opt.isEmpty()) {
            ConsoleUtils.mostrarMensagemErro("Projeto não encontrado!");
            return;
        }
        Project p = opt.get();
        ConsoleUtils.mostrarTitulo("PROJETO: " + p.getNome());
        System.out.println("ID: " + p.getId());
        System.out.println("Descrição: " + p.getDescricao());
        System.out.println("Gerente: " + p.getGerenteId());
        System.out.println("Data Início: " + p.getFormattedDataInicio());
        System.out.println("Previsão Término: " + p.getFormattedDataTerminoPrevista());
        System.out.println("Término Real: " + p.getFormattedDataTerminoReal());
        System.out.println("Status: " + p.getStatus().getDisplayName());
        System.out.println("Equipes vinculadas: " + p.getTeamCount());
        System.out.println("Tarefas vinculadas: " + p.getTaskCount());

        // mostrar histórico do projeto via LogService
        var logs = logService.getEntriesForEntity(p.getId());
        if (!logs.isEmpty()) {
            ConsoleUtils.mostrarSeparador();
            System.out.println("Histórico:");
            for (var l : logs) {
                System.out.println(l.toString());
            }
        }
    }

    private void editarProjeto() {
        String id = ConsoleUtils.lerString("ID do projeto a editar: ");
        Optional<Project> opt = projectRepo.findById(id);
        if (opt.isEmpty()) {
            ConsoleUtils.mostrarMensagemErro("Projeto não encontrado!");
            return;
        }
        Project p = opt.get();
        if (p.isCanceled()) {
            ConsoleUtils.mostrarMensagemErro("Projeto cancelado — não pode ser editado.");
            return;
        }

        if (!authService.hasPermission("EDIT_PROJECT") && !authService.getCurrentUser().getId().equals(p.getGerenteId())) {
            ConsoleUtils.mostrarMensagemErro("Sem permissão para editar este projeto!");
            return;
        }

        String novoNome = ConsoleUtils.lerString("Novo nome (enter para manter): ");
        String novaDesc = ConsoleUtils.lerString("Nova descrição (enter para manter): ");
        String dataInicioStr = ConsoleUtils.lerString("Nova data início (yyyy-MM-dd) ou vazio: ");
        String dataTerminoStr = ConsoleUtils.lerString("Nova data término prevista (yyyy-MM-dd) ou vazio: ");

        if (!novoNome.isEmpty()) p.setNome(novoNome);
        if (!novaDesc.isEmpty()) p.setDescricao(novaDesc);
        if (!dataInicioStr.isEmpty()) p.setDataInicio(LocalDate.parse(dataInicioStr));
        if (!dataTerminoStr.isEmpty()) p.setDataTerminoPrevista(LocalDate.parse(dataTerminoStr));

        projectRepo.save(p);
        logService.log(authService.getCurrentUser().getId(), "EDIT_PROJECT", p.getId(), "Projeto editado");

        ConsoleUtils.mostrarMensagemSucesso("Projeto atualizado.");
    }

    private void cancelarProjeto() {
        String id = ConsoleUtils.lerString("ID do projeto a cancelar: ");
        Optional<Project> opt = projectRepo.findById(id);
        if (opt.isEmpty()) {
            ConsoleUtils.mostrarMensagemErro("Projeto não encontrado!");
            return;
        }
        Project p = opt.get();
        if (p.isCanceled()) {
            ConsoleUtils.mostrarMensagemErro("Projeto já está cancelado.");
            return;
        }

        if (!authService.hasPermission("CANCEL_PROJECT") && !authService.getCurrentUser().getId().equals(p.getGerenteId())) {
            ConsoleUtils.mostrarMensagemErro("Sem permissão para cancelar esse projeto!");
            return;
        }

        p.markAsCanceled();
        projectRepo.save(p);

        // Observação: tarefas serão bloqueadas na TaskView baseada no status do projeto.
        logService.log(authService.getCurrentUser().getId(), "CANCEL_PROJECT", p.getId(), "Projeto cancelado");

        ConsoleUtils.mostrarMensagemSucesso("Projeto cancelado. Ele permanecerá em histórico até remoção.");
    }

    private void reativarProjeto() {
        String id = ConsoleUtils.lerString("ID do projeto a reativar: ");
        Optional<Project> opt = projectRepo.findById(id);
        if (opt.isEmpty()) {
            ConsoleUtils.mostrarMensagemErro("Projeto não encontrado!");
            return;
        }
        Project p = opt.get();
        if (!p.isCanceled()) {
            ConsoleUtils.mostrarMensagemErro("Projeto não está cancelado.");
            return;
        }

        if (!authService.hasPermission("REACTIVATE_PROJECT") && !authService.getCurrentUser().getId().equals(p.getGerenteId())) {
            ConsoleUtils.mostrarMensagemErro("Sem permissão para reativar esse projeto!");
            return;
        }

        p.reactivate();
        projectRepo.save(p);

        logService.log(authService.getCurrentUser().getId(), "REACTIVATE_PROJECT", p.getId(), "Projeto reativado");
        ConsoleUtils.mostrarMensagemSucesso("Projeto reativado e disponível para alterações.");
    }
}

