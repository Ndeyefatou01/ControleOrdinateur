package server;

import java.io.*;
import java.net.*;

public class ClientHandler implements Runnable {

    private Socket socket;
    private final AuthManager auth;

    public ClientHandler(Socket socket, AuthManager auth) {
        this.socket = socket;
        this.auth   = auth;
    }

    @Override
    public void run() {
        String ip = socket.getInetAddress().getHostAddress();
        System.out.println("[Serveur] Connexion depuis " + ip);
        try (
            BufferedReader entree = new BufferedReader(
                new InputStreamReader(socket.getInputStream()));
            PrintWriter sortie = new PrintWriter(
                socket.getOutputStream(), true);
        ) {
             // ── 1. Authentification 
            sortie.println("LOGIN_REQUIRED");

            String login = entree.readLine();
            String mdp   = entree.readLine();

            if (login == null || mdp == null || !auth.authentifier(login, mdp)) {
                sortie.println("AUTH_FAIL");
                System.out.println("[Auth] ECHEC pour '" + login + "' depuis " + ip);
                return;
            }

            // On envoie le rôle au client pour qu'il adapte son interface
            String role = auth.estAdmin(login) ? "ADMIN" : "USER";
            sortie.println("AUTH_OK:" + role);
            System.out.println("[Auth] OK — " + login + " (" + role + ") depuis " + ip);
            
            String commande;
            while ((commande = entree.readLine()) != null) {
                System.out.println("[Thread-" + Thread.currentThread().getName()
                    + " | " + login + "@" + ip + "] > " + commande);
                    
              if (commande.startsWith("SIGNUP ")) {
                    // Seul l'admin peut créer des comptes
                    if (!auth.estAdmin(login)) {
                        sortie.println("[ERREUR] Accès refusé. Seul l'admin peut créer des comptes.");
                        sortie.println("---FIN---");
                    } else {
                        gererSignup(commande, login, sortie);
                    }

                } else {
                    String resultat = executerCommande(commande);
                    sortie.println(resultat);
                    sortie.println("---FIN---");
                }
            }

            System.out.println("[Thread-" + Thread.currentThread().getName()
                 + "] " + login + " déconnecté.");

        } catch (IOException e) {
            System.err.println("[Serveur] Erreur : " + e.getMessage());
        }
    }

       // ── SIGNUP login:mdp 
    private void gererSignup(String commande, String demandeur,
                              PrintWriter sortie) {
        String params = commande.substring(7).trim();

        if (!params.contains(":")) {
            sortie.println("[ERREUR] Syntaxe : SIGNUP login:motdepasse");
            sortie.println("---FIN---");
            return;
        }

        String[] parts      = params.split(":", 2);
        String nouveauLogin = parts[0].trim();
        String nouveauMdp   = parts[1].trim();

        if (nouveauLogin.isEmpty() || nouveauMdp.isEmpty()) {
            sortie.println("[ERREUR] Login et mot de passe ne peuvent pas être vides.");
        } else if (auth.loginExiste(nouveauLogin)) {
            sortie.println("[ERREUR] Le login '" + nouveauLogin + "' existe déjà.");
        } else if (auth.ajouterCompte(nouveauLogin, nouveauMdp, "USER")) {
            sortie.println("[✓] Compte '" + nouveauLogin + "' créé avec succès.");
            System.out.println("[Auth] " + demandeur + " a créé le compte : " + nouveauLogin);
        } else {
            sortie.println("[ERREUR] Impossible de créer le compte.");
        }
        sortie.println("---FIN---");
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