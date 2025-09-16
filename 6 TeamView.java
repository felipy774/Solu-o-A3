package com.projectmanager.view;

import com.projectmanager.model.Team;
import com.projectmanager.model.User;
import com.projectmanager.model.UserProfile;
import com.projectmanager.repository.TeamRepository;
import com.projectmanager.repository.ProjectRepository;
import com.projectmanager.repository.UserRepository;
import com.projectmanager.service.AuthenticationService;
import com.projectmanager.service.LogService;

import java.util.List;
import java.util.Optional;

/**
 * View de console para gerenciar equipes.
 * Aplica regras:
 * - Colaborador só pode estar em 1 equipe.
 * - Gerente/Administrador podem estar em múltiplas equipes.
 * - Criador não pode ser removido como membro (conforme Team implementation).
 * - Só Administrador pode desativar equipe (regra de negócio).
 */
public class TeamView {
    private final TeamRepository teamRepo;
    private final UserRepository userRepo;
    private final ProjectRepository projectRepo;
    private final AuthenticationService authService;
    private final LogService logService;

    public TeamView() {
        this.teamRepo = TeamRepository.getInstance();
        this.userRepo = UserRepository.getInstance();
        this.projectRepo = ProjectRepository.getInstance();
        this.authService = AuthenticationService.getInstance();
        this.logService = LogService.getInstance();
    }

    public void mostrarMenu() {
        boolean voltar = false;
        while (!voltar) {
            ConsoleUtils.limparTela();
            ConsoleUtils.mostrarTitulo("GERENCIAMENTO DE EQUIPES");
            System.out.println("1. ➕ Criar Equipe");
            System.out.println("2. 📋 Listar Equipes");
            System.out.println("3. 🔍 Visualizar Equipe");
            System.out.println("4. ✏️  Editar Equipe");
            System.out.println("5. ➕ Adicionar Membro");
            System.out.println("6. ➖ Remover Membro");
            System.out.println("7. 🚫 Desativar/Ativar Equipe (Admin)");
            System.out.println("0. ⬅️ Voltar");
            int op = ConsoleUtils.lerInt("Escolha uma opção: ");
            switch (op) {
                case 1: criarEquipe(); break;
                case 2: listarEquipes(); break;
                case 3: visualizarEquipe(); break;
                case 4: editarEquipe(); break;
                case 5: adicionarMembro(); break;
                case 6: removerMembro(); break;
                case 7: toggleAtivo(); break;
                case 0: voltar = true; break;
                default: ConsoleUtils.mostrarMensagemErro("Opção inválida!");
            }
            if (!voltar) ConsoleUtils.pausar();
        }
    }

    private void criarEquipe() {
        if (!authService.hasPermission("CREATE_TEAM")) {
            ConsoleUtils.mostrarMensagemErro("Sem permissão para criar equipe!");
            return;
        }

        String nome = ConsoleUtils.lerString("Nome da equipe: ");
        String desc = ConsoleUtils.lerString("Descrição: ");
        String criadorId = authService.isLoggedIn() ? authService.getCurrentUser().getId() : ConsoleUtils.lerString("ID do criador: ");

        Team team = new Team(nome, desc, criadorId);
        teamRepo.save(team);

        logService.log(authService.getCurrentUser().getId(), "CREATE_TEAM", team.getId(), "Equipe criada: " + nome);
        ConsoleUtils.mostrarMensagemSucesso("Equipe criada com ID: " + team.getId());
    }

    private void listarEquipes() {
        List<Team> teams = teamRepo.findAll();
        ConsoleUtils.mostrarTitulo("EQUIPES CADASTRADAS");
        if (teams.isEmpty()) {
            System.out.println("Nenhuma equipe cadastrada.");
            return;
        }
        for (Team t : teams) {
            System.out.println(t.toString());
        }
    }

    private void visualizarEquipe() {
        String id = ConsoleUtils.lerString("ID da equipe: ");
        Optional<Team> opt = teamRepo.findById(id);
        if (opt.isEmpty()) {
            ConsoleUtils.mostrarMensagemErro("Equipe não encontrada!");
            return;
        }
        Team t = opt.get();
        ConsoleUtils.mostrarTitulo("EQUIPE: " + t.getNome());
        System.out.println("ID: " + t.getId());
        System.out.println("Descrição: " + t.getDescricao());
        System.out.println("Criador: " + t.getCriadorId());
        System.out.println("Ativa: " + (t.isAtivo() ? "Sim" : "Não"));
        System.out.println("Membros: " + t.getMemberIds().size());
        System.out.println("Projetos vinculados: " + t.getProjectCount());

        // Mostrar membros com dados minimamente identificáveis
        for (String uid : t.getMemberIds()) {
            userRepo.findById(uid).ifPresent(user -> System.out.println(" - " + user.getNomeCompleto() + " (" + user.getId() + ")"));
        }

        var logs = logService.getEntriesForEntity(t.getId());
        if (!logs.isEmpty()) {
            ConsoleUtils.mostrarSeparador();
            System.out.println("Histórico:");
            for (var l : logs) System.out.println(l.toString());
        }
    }

    private void editarEquipe() {
        String id = ConsoleUtils.lerString("ID da equipe a editar: ");
        Optional<Team> opt = teamRepo.findById(id);
        if (opt.isEmpty()) {
            ConsoleUtils.mostrarMensagemErro("Equipe não encontrada!");
            return;
        }
        Team t = opt.get();
        // Apenas criador ou admin podem alterar nome/descrição
        boolean isCreator = authService.isLoggedIn() && authService.getCurrentUser().getId().equals(t.getCriadorId());
        boolean isAdmin = authService.hasPermission("ADMIN");
        if (!isCreator && !isAdmin) {
            ConsoleUtils.mostrarMensagemErro("Somente o criador ou administrador pode editar a equipe!");
            return;
        }

        String novoNome = ConsoleUtils.lerString("Novo nome (enter para manter): ");
        String novaDesc = ConsoleUtils.lerString("Nova descrição (enter para manter): ");
        if (!novoNome.isEmpty()) t.setNome(novoNome);
        if (!novaDesc.isEmpty()) t.setDescricao(novaDesc);
        teamRepo.save(t);
        logService.log(authService.getCurrentUser().getId(), "EDIT_TEAM", t.getId(), "Equipe editada");
        ConsoleUtils.mostrarMensagemSucesso("Equipe atualizada.");
    }

    private void adicionarMembro() {
        String teamId = ConsoleUtils.lerString("ID da equipe: ");
        Optional<Team> optTeam = teamRepo.findById(teamId);
        if (optTeam.isEmpty()) { ConsoleUtils.mostrarMensagemErro("Equipe não encontrada!"); return; }
        Team team = optTeam.get();

        String userId = ConsoleUtils.lerString("ID do usuário a adicionar: ");
        Optional<User> optUser = userRepo.findById(userId);
        if (optUser.isEmpty()) { ConsoleUtils.mostrarMensagemErro("Usuário não encontrado!"); return; }
        User user = optUser.get();

        // regra: colaborador só pode estar em 1 equipe; gerente/admin podem em várias
        UserProfile perfil = user.getPerfil();
        if (perfil == UserProfile.COLABORADOR) {
            var teamsOfUser = teamRepo.findByMemberId(userId);
            if (!teamsOfUser.isEmpty()) {
                ConsoleUtils.mostrarMensagemErro("Usuário é Colaborador e já pertence a uma equipe. Remova-o da outra equipe primeiro.");
                return;
            }
        }
        if (team.addMember(userId)) {
            teamRepo.save(team);
            logService.log(authService.getCurrentUser().getId(), "ADD_TEAM_MEMBER", team.getId(), "adicionado membro=" + userId);
            ConsoleUtils.mostrarMensagemSucesso("Membro adicionado.");
        } else {
            ConsoleUtils.mostrarMensagemErro("Usuário já é membro desta equipe.");
        }
    }

    private void removerMembro() {
        String teamId = ConsoleUtils.lerString("ID da equipe: ");
        Optional<Team> optTeam = teamRepo.findById(teamId);
        if (optTeam.isEmpty()) { ConsoleUtils.mostrarMensagemErro("Equipe não encontrada!"); return; }
        Team team = optTeam.get();

        String userId = ConsoleUtils.lerString("ID do usuário a remover: ");
        if (userId.equals(team.getCriadorId())) {
            ConsoleUtils.mostrarMensagemErro("Não é permitido remover o criador da equipe.");
            return;
        }

        boolean performedByCreator = authService.isLoggedIn() && authService.getCurrentUser().getId().equals(team.getCriadorId());
        boolean isAdmin = authService.hasPermission("ADMIN");
        boolean isSelf = authService.isLoggedIn() && authService.getCurrentUser().getId().equals(userId);

        // regra: criador pode adicionar/remover; colaborador pode sair do grupo por si mesmo; admin pode desativar/ativar equipe
        if (!(performedByCreator || isAdmin || isSelf)) {
            ConsoleUtils.mostrarMensagemErro("Somente o criador, administrador ou o próprio usuário pode removê-lo.");
            return;
        }

        if (team.removeMember(userId)) {
            teamRepo.save(team);
            logService.log(authService.getCurrentUser().getId(), "REMOVE_TEAM_MEMBER", team.getId(), "removido membro=" + userId);
            ConsoleUtils.mostrarMensagemSucesso("Membro removido.");
        } else {
            ConsoleUtils.mostrarMensagemErro("Usuário não é membro desta equipe.");
        }
    }

    private void toggleAtivo() {
        if (!authService.hasPermission("ADMIN")) {
            ConsoleUtils.mostrarMensagemErro("Apenas administrador pode ativar/desativar equipes.");
            return;
        }
        String teamId = ConsoleUtils.lerString("ID da equipe: ");
        Optional<Team> opt = teamRepo.findById(teamId);
        if (opt.isEmpty()) { ConsoleUtils.mostrarMensagemErro("Equipe não encontrada!"); return; }
        Team t = opt.get();
        t.setAtivo(!t.isAtivo());
        teamRepo.save(t);
        logService.log(authService.getCurrentUser().getId(), "TOGGLE_TEAM_ACTIVE", t.getId(), "ativo=" + t.isAtivo());
        ConsoleUtils.mostrarMensagemSucesso("Equipe agora está " + (t.isAtivo() ? "ativa" : "inativa"));
    }
}
