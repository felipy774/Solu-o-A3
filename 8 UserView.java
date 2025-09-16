package com.projectmanager.view;

import com.projectmanager.model.User;
import com.projectmanager.model.UserProfile;
import com.projectmanager.repository.UserRepository;
import com.projectmanager.repository.TeamRepository;
import com.projectmanager.service.AuthenticationService;
import com.projectmanager.service.LogService;

public class UserView {
    private UserRepository userRepository;
    private AuthenticationService authService;
    private TeamRepository teamRepo;
    private LogService logService;

    public UserView() {
        this.userRepository = UserRepository.getInstance();
        this.authService = AuthenticationService.getInstance();
        this.teamRepo = TeamRepository.getInstance();
        this.logService = LogService.getInstance();
    }

    public void mostrarMenu() {
        boolean voltar = false;
        
        while (!voltar) {
            ConsoleUtils.limparTela();
            ConsoleUtils.mostrarTitulo("GERENCIAMENTO DE USUÁRIOS");
            
            if (authService.hasPermission("MANAGE_USERS")) {
                System.out.println("1. 👤 Cadastrar Usuário");
                System.out.println("2. 📋 Listar Usuários");
                System.out.println("3. 🔍 Buscar Usuário");
                System.out.println("4. ✏️  Editar Usuário");
                System.out.println("5. 🗑️  Desativar Usuário");
            }
            
            System.out.println("6. 👁️  Visualizar Meu Perfil");
            System.out.println("7. 🔧 Alterar Minha Senha");
            System.out.println("0. ⬅️  Voltar ao Menu Principal");
            
            int opcao = ConsoleUtils.lerInt("Escolha uma opção: ");
            
            switch (opcao) {
                case 1:
                    if (authService.hasPermission("MANAGE_USERS")) {
                        cadastrarUsuario();
                    } else {
                        ConsoleUtils.mostrarMensagemErro("Sem permissão para esta ação!");
                    }
                    break;
                case 2:
                    if (authService.hasPermission("MANAGE_USERS")) {
                        listarUsuarios();
                    } else {
                        ConsoleUtils.mostrarMensagemErro("Sem permissão para esta ação!");
                    }
                    break;
                case 6:
                    visualizarMeuPerfil();
                    break;
                case 7:
                    alterarSenha();
                    break;
                case 0:
                    voltar = true;
                    break;
                default:
                    ConsoleUtils.mostrarMensagemErro("Opção inválida ou em desenvolvimento!");
            }
            
            if (!voltar) {
                ConsoleUtils.pausar();
            }
        }
    }

    public void fazerLogin() {
        ConsoleUtils.mostrarTitulo("LOGIN DO USUÁRIO");
        
        String login = ConsoleUtils.lerString("Login: ");
        String senha = ConsoleUtils.lerString("Senha: ");
        
        if (authService.login(login, senha)) {
            ConsoleUtils.mostrarMensagemSucesso("Login realizado com sucesso!");
            System.out.println("Bem-vindo(a), " + authService.getCurrentUser().getNomeCompleto() + "!");
            logService.log(authService.getCurrentUser().getId(), "LOGIN", "USER", "Login realizado");
        } else {
            ConsoleUtils.mostrarMensagemErro("Login ou senha incorretos, ou usuário inativo!");
        }
    }

    public void cadastrarUsuario() {
        ConsoleUtils.mostrarTitulo("CADASTRO DE USUÁRIO");
        
        try {
            String nome = ConsoleUtils.lerString("Nome completo: ");
            if (nome.isEmpty()) {
                ConsoleUtils.mostrarMensagemErro("Nome é obrigatório!");
                return;
            }

            String cpf = ConsoleUtils.lerString("CPF: ");
            if (!User.isValidCPF(cpf)) {
                ConsoleUtils.mostrarMensagemErro("CPF inválido!");
                return;
            }
            cpf = User.formatCPF(cpf);

            if (userRepository.findByCpf(cpf).isPresent()) {
                ConsoleUtils.mostrarMensagemErro("CPF já cadastrado!");
                return;
            }

            String email = ConsoleUtils.lerString("Email: ");
            if (!User.isValidEmail(email)) {
                ConsoleUtils.mostrarMensagemErro("Email inválido!");
                return;
            }

            if (userRepository.findByEmail(email).isPresent()) {
                ConsoleUtils.mostrarMensagemErro("Email já cadastrado!");
                return;
            }

            String cargo = ConsoleUtils.lerString("Cargo: ");
            if (cargo.isEmpty()) {
                ConsoleUtils.mostrarMensagemErro("Cargo é obrigatório!");
                return;
            }

            String login = ConsoleUtils.lerString("Login: ");
            if (login.isEmpty()) {
                ConsoleUtils.mostrarMensagemErro("Login é obrigatório!");
                return;
            }

            if (userRepository.findByLogin(login).isPresent()) {
                ConsoleUtils.mostrarMensagemErro("Login já existe!");
                return;
            }

            String senha = ConsoleUtils.lerString("Senha: ");
            if (senha.length() < 6) {
                ConsoleUtils.mostrarMensagemErro("Senha deve ter pelo menos 6 caracteres!");
                return;
            }

            System.out.println("\nPerfis disponíveis:");
            UserProfile[] perfis = UserProfile.values();
            for (int i = 0; i < perfis.length; i++) {
                System.out.println((i + 1) + ". " + perfis[i].getDisplayName());
            }

            int opcaoPerfil = ConsoleUtils.lerInt("Escolha o perfil (número): ");
            if (opcaoPerfil < 1 || opcaoPerfil > perfis.length) {
                ConsoleUtils.mostrarMensagemErro("Perfil inválido!");
                return;
            }

            UserProfile perfil = perfis[opcaoPerfil - 1];
            
            // Verificar se o usuário atual tem permissão para criar esse perfil
            if (authService.isLoggedIn() && !authService.getCurrentUser().getPerfil().canManageUser(perfil)) {
                ConsoleUtils.mostrarMensagemErro("Sem permissão para criar usuário com este perfil!");
                return;
            }

            User novoUsuario = new User(nome, cpf, email, cargo, login, senha, perfil);
            userRepository.save(novoUsuario);

            logService.log(authService.isLoggedIn() ? authService.getCurrentUser().getId() : "SYSTEM",
                    "CREATE_USER", novoUsuario.getId(), "Usuário criado com perfil=" + perfil.name());

            ConsoleUtils.mostrarMensagemSucesso("Usuário cadastrado com sucesso!");
            System.out.println("ID: " + novoUsuario.getId());

        } catch (Exception e) {
            ConsoleUtils.mostrarMensagemErro("Erro ao cadastrar usuário: " + e.getMessage());
        }
    }

    public void listarUsuarios() {
        ConsoleUtils.mostrarTitulo("LISTA DE USUÁRIOS");
        
        var usuarios = userRepository.findAll();
        
        if (usuarios.isEmpty()) {
            System.out.println("Nenhum usuário cadastrado.");
            return;
        }

        System.out.printf("%-15s %-30s %-15s %-20s %-10s%n", 
                         "ID", "Nome", "Login", "Email", "Status");
        ConsoleUtils.mostrarSeparador();

        for (User user : usuarios) {
            System.out.printf("%-15s %-30s %-15s %-20s %-10s%n",
                             user.getId().substring(0, Math.min(user.getId().length(), 12)) + "...",
                             user.getNomeCompleto().length() > 28 ? 
                                 user.getNomeCompleto().substring(0, 25) + "..." : user.getNomeCompleto(),
                             user.getLogin(),
                             user.getEmail().length() > 18 ? 
                                 user.getEmail().substring(0, 15) + "..." : user.getEmail(),
                             user.isAtivo() ? "Ativo" : "Inativo");
        }
    }

    public void visualizarMeuPerfil() {
        ConsoleUtils.mostrarTitulo("MEU PERFIL");
        
        User user = authService.getCurrentUser();
        
        System.out.println("ID: " + user.getId());
        System.out.println("Nome: " + user.getNomeCompleto());
        System.out.println("CPF: " + user.getCpf());
        System.out.println("Email: " + user.getEmail());
        System.out.println("Cargo: " + user.getCargo());
        System.out.println("Login: " + user.getLogin());
        System.out.println("Perfil: " + user.getPerfil().getDisplayName());
        System.out.println("Data de Criação: " + user.getDataCriacao().format(java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")));
        System.out.println("Status: " + (user.isAtivo() ? "Ativo" : "Inativo"));
        System.out.println("Equipes: " + user.getTeamIds().size());
        System.out.println("Projetos Gerenciados: " + user.getProjectIds().size());

        // listar as equipes do usuário
        var teams = teamRepo.findByMemberId(user.getId());
        if (!teams.isEmpty()) {
            System.out.println("\nEquipes do usuário:");
            for (var t : teams) {
                System.out.println(" - " + t.getNome() + " (" + t.getId() + ")");
            }
        }
    }

    public void alterarSenha() {
        ConsoleUtils.mostrarTitulo("ALTERAR SENHA");
        
        String senhaAtual = ConsoleUtils.lerString("Senha atual: ");
        
        if (!authService.getCurrentUser().getSenha().equals(senhaAtual)) {
            ConsoleUtils.mostrarMensagemErro("Senha atual incorreta!");
            return;
        }

        String novaSenha = ConsoleUtils.lerString("Nova senha: ");
        if (novaSenha.length() < 6) {
            ConsoleUtils.mostrarMensagemErro("Nova senha deve ter pelo menos 6 caracteres!");
            return;
        }

        String confirmacao = ConsoleUtils.lerString("Confirme a nova senha: ");
        if (!novaSenha.equals(confirmacao)) {
            ConsoleUtils.mostrarMensagemErro("Senhas não conferem!");
            return;
        }

        User user = authService.getCurrentUser();
        user.setSenha(novaSenha);
        userRepository.save(user);

        logService.log(user.getId(), "CHANGE_PASSWORD", "USER", "Senha alterada");

        ConsoleUtils.mostrarMensagemSucesso("Senha alterada com sucesso!");
    }

    public void criarAdminPadrao() {
        // Criar usuário administrador padrão se não existir nenhum usuário
        if (userRepository.findAll().isEmpty()) {
            User admin = new User("Administrador do Sistema", "00000000000", 
                                 "admin@sistema.com", "Administrador", 
                                 "admin", "123456", UserProfile.ADMINISTRADOR);
            userRepository.save(admin);
            
            System.out.println("\n🔧 Usuário administrador padrão criado:");
            System.out.println("Login: admin");
            System.out.println("Senha: 123456");
            System.out.println("⚠️ Altere a senha após o primeiro login!\n");
        }
    }
}
