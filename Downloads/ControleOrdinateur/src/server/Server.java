package server;

import java.io.*;
import java.net.*;

public class Server {

    private static final int PORT = 5000;

    public static void main(String[] args) {
        System.out.println("=== Serveur démarré sur le port " + PORT + " ===");
        AuthManager auth = new AuthManager();

        try (ServerSocket serverSocket = new ServerSocket(PORT)) {

            while (true) {
                System.out.println("[Serveur] En attente d'un client...");

                Socket clientSocket = serverSocket.accept();
                String ip = clientSocket.getInetAddress().getHostAddress();
                System.out.println("[Serveur] Connexion de : " + ip);

                Thread thread = new Thread(new ClientHandler(clientSocket, auth));
                 thread.setName("Client-" + ip);
                thread.start();

                System.out.println("[Serveur] Thread lancé : Thread-" 
                    + thread.getName());
            }

        } catch (IOException e) {
            System.err.println("[Serveur] Erreur : " + e.getMessage());
        }
    }
}