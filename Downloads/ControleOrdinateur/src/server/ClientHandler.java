package server;

import java.io.*;
import java.net.*;

public class ClientHandler implements Runnable {

    private Socket socket;

    public ClientHandler(Socket socket) {
        this.socket = socket;
    }

    @Override
    public void run() {
        try (
            BufferedReader entree = new BufferedReader(
                new InputStreamReader(socket.getInputStream()));
            PrintWriter sortie = new PrintWriter(
                socket.getOutputStream(), true);
        ) {
            String commande;
            while ((commande = entree.readLine()) != null) {
                System.out.println("[Thread-" + Thread.currentThread().getName()
                    + "] Commande reçue : " + commande);
                String resultat = executerCommande(commande);
                sortie.println(resultat);
                sortie.println("---FIN---");
            }
            System.out.println("[Thread-" + Thread.currentThread().getName()
                + "] Client déconnecté.");

        } catch (IOException e) {
            System.err.println("[Serveur] Erreur : " + e.getMessage());
        }
    }

    private String executerCommande(String commande) {
        StringBuilder resultat = new StringBuilder();
        try {
            String os = System.getProperty("os.name").toLowerCase();
            ProcessBuilder pb;

            if (os.contains("win")) {
                pb = new ProcessBuilder("cmd.exe", "/c", commande);
            } else {
                pb = new ProcessBuilder("/bin/sh", "-c", commande);
            }

            pb.redirectErrorStream(true);
            Process process = pb.start();

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