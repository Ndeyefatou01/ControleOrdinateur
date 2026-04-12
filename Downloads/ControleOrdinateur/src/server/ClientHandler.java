package server;

import java.io.*;
import java.net.*;

public class ClientHandler implements Runnable {

    private final Socket socket;
    private final AuthManager auth;
    private static final String FIN          = "---FIN---";
    private static final String FIN_FICHIER  = "---FIN_FICHIER---";
    
    public ClientHandler(Socket socket, AuthManager auth) {
        this.socket = socket;
        this.auth   = auth;
    }

    @Override
    public void run() {
        String ip = socket.getInetAddress().getHostAddress();
       Logger.info("[Serveur] Connexion depuis " + ip);
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
               Logger.warn("[Auth] ECHEC pour '" + login + "' depuis " + ip);
                return;
            }

            // On envoie le rôle au client pour qu'il adapte son interface
            String role = auth.estAdmin(login) ? "ADMIN" : "USER";
            sortie.println("AUTH_OK:" + role);
           Logger.info("[Auth] OK — " + login + " (" + role + ") depuis " + ip);
            
            String commande;
            while ((commande = entree.readLine()) != null) {
               Logger.info("[Thread-" + Thread.currentThread().getName()
                    + " | " + login + "@" + ip + "] > " + commande);
                    
              if (commande.startsWith("SIGNUP ")) {
                    if (!auth.estAdmin(login)) {
                        sortie.println("[ERREUR] Accès refusé. Seul l'admin peut créer des comptes.");
                        sortie.println(FIN);
                    } else {
                        gererSignup(commande, login, sortie);
                    }
                
                } else if (commande.startsWith("UPLOAD ")) {
                    String nomFichier = commande.substring(7).trim();
                    recevoirFichier(nomFichier, entree, sortie);

                } else if (commande.startsWith("DOWNLOAD ")) {
                    String nomFichier = commande.substring(9).trim();
                    envoyerFichier(nomFichier, sortie);

                } else {
                    String resultat = executerCommande(commande);
                    sortie.println(resultat);
                    sortie.println(FIN);
                }
            }

           Logger.info("[Thread-" + Thread.currentThread().getName()
                 + "] " + login + " déconnecté.");

        } catch (IOException e) {
            Logger.erreur("[Serveur] Erreur : " + e.getMessage());
        }
    }

       // ── SIGNUP login:mdp 
    private void gererSignup(String commande, String demandeur,
                              PrintWriter sortie) {
        String params = commande.substring(7).trim();

        if (!params.contains(":")) {
            sortie.println("[ERREUR] Syntaxe : SIGNUP login:motdepasse");
            sortie.println(FIN);
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
           Logger.info("[Auth] " + demandeur + " a créé le compte : " + nouveauLogin);
        } else {
            sortie.println("[ERREUR] Impossible de créer le compte.");
        }
        sortie.println(FIN);
    }

    //Réception d'un fichier envoyé par le client (UPLOAD)
private void recevoirFichier(String nomFichier, BufferedReader entree,
                              PrintWriter sortie) throws IOException {
    File dossier = new File("uploads");
    if (!dossier.exists()) dossier.mkdirs();

    File fichier = new File(dossier, nomFichier);
    try (BufferedWriter bw = new BufferedWriter(new FileWriter(fichier))) {
        String ligne;
        while ((ligne = entree.readLine()) != null) {
            if (ligne.equals(FIN_FICHIER)) break;
            bw.write(ligne);
            bw.newLine();
        }
    }
    Logger.info("Fichier reçu : uploads/" + nomFichier);
    sortie.println("[✓] Fichier '" + nomFichier + "' uploadé avec succès.");
    sortie.println(FIN);
}

//Envoi d'un fichier au client (DOWNLOAD) 
private void envoyerFichier(String nomFichier, PrintWriter sortie) throws IOException {
    File fichier = new File("uploads", nomFichier);
    if (!fichier.exists()) fichier = new File(nomFichier);

    if (!fichier.exists()) {
        sortie.println("[ERREUR] Fichier introuvable : " + nomFichier);
        sortie.println(FIN);
        Logger.warn("DOWNLOAD demandé mais fichier introuvable : " + nomFichier);
        return;
    }

    Logger.info("Envoi du fichier : " + fichier.getPath());
    try (BufferedReader br = new BufferedReader(new FileReader(fichier))) {
        String ligne;
        while ((ligne = br.readLine()) != null)
            sortie.println(ligne);
    }
    sortie.println(FIN_FICHIER);
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