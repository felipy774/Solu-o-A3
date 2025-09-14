package com.projectmanager.view;

import java.util.Scanner;

public class ConsoleUtils {
    private static Scanner scanner = new Scanner(System.in);

    public static String lerString(String prompt) {
        System.out.print(prompt);
        return scanner.nextLine().trim();
    }

    public static int lerInt(String prompt) {
        while (true) {
            try {
                System.out.print(prompt);
                String input = scanner.nextLine().trim();
                return Integer.parseInt(input);
            } catch (NumberFormatException e) {
                System.out.println("Por favor, digite um número válido.");
            }
        }
    }

    public static void pausar() {
        System.out.println("\nPressione Enter para continuar...");
        scanner.nextLine();
    }

    public static void limparTela() {
        try {
            if (System.getProperty("os.name").contains("Windows")) {
                new ProcessBuilder("cmd", "/c", "cls").inheritIO().start().waitFor();
            } else {
                System.out.print("\033[H\033[2J");
                System.out.flush();
            }
        } catch (Exception e) {
            for (int i = 0; i < 50; i++) {
                System.out.println();
            }
        }
    }

    public static void mostrarSeparador() {
        System.out.println("=" + "=".repeat(70) + "=");
    }

    public static void mostrarTitulo(String titulo) {
        mostrarSeparador();
        System.out.println("  " + titulo.toUpperCase());
        mostrarSeparador();
    }

    public static void mostrarMensagemSucesso(String mensagem) {
        System.out.println("✓ " + mensagem);
    }

    public static void mostrarMensagemErro(String mensagem) {
        System.out.println("✗ " + mensagem);
    }

    public static void fecharScanner() {
        scanner.close();
    }
}
