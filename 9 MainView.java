package com.projectmanager.view;

import com.projectmanager.service.AuthenticationService;

public class MainView {
    private AuthenticationService authService;
    private UserView userView;
    private ProjectView projectView;
    private TeamView teamView;
    private TaskView taskView;

    public MainView() {
        this.authService = AuthenticationService.getInstance();
        this.userView = new UserView();
        this.projectView = new ProjectView();
        this.teamView = new TeamView();
        this.taskView = new TaskView();
    }

    public void iniciar() {
        ConsoleUtils.mostrarTitulo("SISTEMA DE GERENCIAMENTO DE PROJETOS E EQUIPES");
        System.out.println("Sistema completo de gerenciamento com hierarquia de usuários,");
        System.out.println("controle de projetos, equipes e tarefas integradas!");
        
        // Criar usuário administrador padrão se não existir
        userView.criarAdminPadrao();
        
        boolean continuar = true;
        
        while (continuar) {
            if (!authService.isLoggedIn()) {
                mostrarMenuLogin();
                int opcao = ConsoleUtils.lerInt("Escolha uma opção: ");
                
                switch (opcao) {
                    case 1:
                        userView.fazerLogin();
                        break;
                    case 2:
                        userView.cadastrarUsuario();
                        break;
                    case 3:
                        mostrarSobre();
                        break;
                    case 0:
                        continuar = false;
                        break;
                    default:
                        ConsoleUtils.mostrarMensagemErro("Opção inválida!");
                }
            } else {
                mostrarMenuPrincipal();
                int opcao = ConsoleUtils.lerInt("Escolha uma opção: ");
                
                switch (opcao) {
                    case 1:
                        menuUsuarios();
                        break;
                    case 2:
                        menuProjetos();
                        break;
                    case 3:
                        menuEquipes();
                        break;
                    case 4:
                        menuTarefas();
                        break;
                    case 5:
                        mostrarEstatisticas();
                        break;
                    case 6:
                        authService.logout();
                        ConsoleUtils.mostrarMensagemSucesso("Logout realizado com sucesso!");
                        break;
                    case 0:
                        continuar = false;
                        break;
                    default:
                        ConsoleUtils.mostrarMensagemErro("Opção inválida!");
                }
            }
            
            if (continuar) {
                ConsoleUtils.pausar();
                ConsoleUtils.limparTela();
            }
        }
        
        encerrarSistema();
    }

    private void mostrarMenuLogin() {
        ConsoleUtils.mostrarTitulo("LOGIN NECESSÁRIO");
        System.out.println("1. 🔐 Fazer Login");
        System.out.println("2. 👤 Cadastrar Usuário");
        System.out.println("3. ℹ️  Sobre o Sistema");
        System.out.println("0. 🚪 Sair");
        System.out.println();
    }

    private void mostrarMenuPrincipal() {
        ConsoleUtils.mostrarTitulo("MENU PRINCIPAL");
        
        System.out.println("🟢 Usuário: " + authService.getCurrentUser().getNomeCompleto());
        System.out.println("📋 Perfil: " + authService.getCurrentUser().getPerfil().getDisplayName());
        System.out.println();
        
        System.out.println("1. 👥 Gerenciar Usuários");
        System.out.println("2. 📂 Gerenciar Projetos");
        System.out.println("3. 👨‍👩‍👧‍👦 Gerenciar Equipes");
        System.out.println("4. ✅ Gerenciar Tarefas");
        System.out.println("5. 📊 Estatísticas e Relatórios");
        System.out.println("6. 🚪 Logout");
        System.out.println("0. 🚪 Sair do Sistema");
        System.out.println();
    }

    private void menuUsuarios() {
        userView.mostrarMenu();
    }

    private void menuProjetos() {
        projectView.mostrarMenu();
    }

    private void menuEquipes() {
        teamView.mostrarMenu();
    }

    private void menuTarefas() {
        taskView.mostrarMenu();
    }

    private void mostrarEstatisticas() {
        ConsoleUtils.mostrarTitulo("ESTATÍSTICAS DO SISTEMA");
        // Estatísticas simples (valores em memória)
        int usuarios = com.projectmanager.repository.UserRepository.getInstance().findAll().size();
        int projetos = com.projectmanager.repository.ProjectRepository.getInstance().findAll().size();
        int equipes = com.projectmanager.repository.TeamRepository.getInstance().findAll().size();
        int tarefas = com.projectmanager.repository.TaskRepository.getInstance().findAll().size();

        System.out.println("Usuários cadastrados: " + usuarios);
        System.out.println("Projetos cadastrados: " + projetos);
        System.out.println("Equipes cadastradas: " + equipes);
        System.out.println("Tarefas cadastradas: " + tarefas);
    }

    private void mostrarSobre() {
        ConsoleUtils.mostrarTitulo("SOBRE O SISTEMA");
        System.out.println("🎯 Sistema de Gerenciamento de Projetos e Equipes");
        System.out.println();
        System.out.println("📋 Funcionalidades Principais:");
        System.out.println("  • Cadastro de usuários com perfis hierárquicos");
        System.out.println("  • Gerenciamento de projetos com status e prazos");
        System.out.println("  • Criação e administração de equipes");
        System.out.println("  • Sistema de tarefas com validações obrigatórias");
        System.out.println("  • Logs completos de atividades do sistema");
        System.out.println("  • Controle de permissões por perfil de usuário");
        System.out.println();
        System.out.println("👥 Perfis de Usuário:");
        System.out.println("  • Administrador: Controle total do sistema");
        System.out.println("  • Gerente: Gerencia projetos e equipes");
        System.out.println("  • Colaborador: Executa tarefas atribuídas");
        System.out.println();
        System.out.println("🏗️  Arquitetura MVC com persistência em arquivos (simulada em memória)");
    }

    private void encerrarSistema() {
        ConsoleUtils.mostrarTitulo("ENCERRANDO SISTEMA");
        
        if (authService.isLoggedIn()) {
            authService.logout();
        }
        
        System.out.println("Obrigado por usar o Sistema de Gerenciamento!");
        System.out.println("Até a próxima! 🚀");
        
        ConsoleUtils.fecharScanner();
    }
}
