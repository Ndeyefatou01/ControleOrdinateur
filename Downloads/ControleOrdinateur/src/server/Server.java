package server;

import java.io.*;
import java.net.*;

public class Server {

    // Le port sur lequel le serveur écoute
    private static final int PORT = 5000;

    public static void main(String[] args) {
        System.out.println("=== Serveur démarré sur le port " + PORT + " ===");

        try (ServerSocket serverSocket = new ServerSocket(PORT)) {

            // Boucle infinie : on attend des clients en continu
            while (true) {
                System.out.println("[Serveur] En attente d'un client...");

                // On accepte la connexion d'un client
                Socket clientSocket = serverSocket.accept();
                System.out.println("[Serveur] Client connecté : " 
                    + clientSocket.getInetAddress().getHostAddress());

                // On lance un thread pour ce client (Membre 2 s'en occupera)
                // Pour l'instant, on gère un seul client à la fois
                gererClient(clientSocket);
            }

        } catch (IOException e) {
            System.err.println("[Serveur] Erreur : " + e.getMessage());
        }
    }

    private static void gererClient(Socket socket) {
        try (
            // Flux pour lire ce que le client envoie
            BufferedReader entree = new BufferedReader(
                new InputStreamReader(socket.getInputStream()));

            // Flux pour envoyer la réponse au client
            PrintWriter sortie = new PrintWriter(
                socket.getOutputStream(), true);
        ) {
            String commande;

            // On lit les commandes du client une par une
            while ((commande = entree.readLine()) != null) {
                System.out.println("[Serveur] Commande reçue : " + commande);

                // On exécute la commande et on envoie le résultat
                String resultat = executerCommande(commande);
                sortie.println(resultat);
                sortie.println("---FIN---"); // Signal de fin de résultat
            }

        } catch (IOException e) {
            System.err.println("[Serveur] Erreur client : " + e.getMessage());
        }
    }

    private static String executerCommande(String commande) {
        StringBuilder resultat = new StringBuilder();

        try {
            // Détecter le système d'exploitation
            String os = System.getProperty("os.name").toLowerCase();
            ProcessBuilder pb;

            if (os.contains("win")) {
                pb = new ProcessBuilder("cmd.exe", "/c", commande);
            } else {
                pb = new ProcessBuilder("/bin/sh", "-c", commande);
            }

            pb.redirectErrorStream(true); // Mélange stdout + stderr
            Process process = pb.start();

            // Lire le résultat de la commande
            BufferedReader reader = new BufferedReader(
                new InputStreamReader(process.getInputStream()));

            String ligne;
            while ((ligne = reader.readLine()) != null) {
                resultat.append(ligne).append("\n");
            }

            process.waitFor();

        } catch (Exception e) {
            resultat.append("[ERREUR] ").append(e.getMessage());
        }

        return resultat.toString().trim();
    }
}