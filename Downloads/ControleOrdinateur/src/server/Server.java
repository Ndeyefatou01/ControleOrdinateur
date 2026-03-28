package server;

import java.io.*;
import java.net.*;

public class Server {

    private static final int PORT = 5000;

    public static void main(String[] args) {
        System.out.println("=== Serveur démarré sur le port " + PORT + " ===");

        try (ServerSocket serverSocket = new ServerSocket(PORT)) {

            while (true) {
                System.out.println("[Serveur] En attente d'un client...");

                // On accepte la connexion
                Socket clientSocket = serverSocket.accept();
                System.out.println("[Serveur] Client connecté : "
                    + clientSocket.getInetAddress().getHostAddress());

                // On crée un thread pour ce client
                Thread thread = new Thread(new ClientHandler(clientSocket));
                thread.start();

                System.out.println("[Serveur] Thread lancé : Thread-" 
                    + thread.getId());
            }

        } catch (IOException e) {
            System.err.println("[Serveur] Erreur : " + e.getMessage());
        }
    }
}