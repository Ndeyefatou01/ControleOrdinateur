package client;

import java.io.*;
import java.net.*;
import java.util.Scanner;

public class Client {

private static final String FIN         = "---FIN---";
private static final String FIN_FICHIER = "---FIN_FICHIER---";
private static final String ADDR_SERVEUR = "127.0.0.1";
private static final int    PORT         = 5000;
    public static void main(String[] args) {

        System.out.println("=== Client démarré ===");
        System.out.println("Connexion à " + ADDR_SERVEUR + ":" + PORT);

        try (
            Socket socket = new Socket(ADDR_SERVEUR, PORT);
            PrintWriter envoi = new PrintWriter(
                socket.getOutputStream(), true);
            BufferedReader reception = new BufferedReader(
                new InputStreamReader(socket.getInputStream()));
            Scanner scanner = new Scanner(System.in);
        ) {
            System.out.println("[Client] Connecté au serveur !");
            System.out.println("Tapez une commande (ex: ls ou dir). 'exit' pour quitter.\n");
            
             // ── Authentification
            String msg = reception.readLine();
            if (!"LOGIN_REQUIRED".equals(msg)) {
                System.err.println("[Client] Protocole inattendu : " + msg);
                return;
            }

            System.out.print("Login        : ");
            String login = scanner.nextLine();
            System.out.print("Mot de passe : ");
            String mdp = scanner.nextLine();

            envoi.println(login);
            envoi.println(mdp);

            String reponse = reception.readLine();

            if (reponse == null || reponse.equals("AUTH_FAIL")) {
                System.err.println("[Client] Identifiants incorrects.");
                return;
            }

            String role = reponse.contains(":") ? reponse.split(":")[1] : "USER";
            boolean estAdmin = "ADMIN".equals(role);

            System.out.println("[Client] ✓ Connecté en tant que : "
                    + login + " [" + role + "]");

            if (estAdmin) {
                System.out.println("Commandes admin : SIGNUP login:mdp, dir, ipconfig, exit");
            } else {
                System.out.println("Commandes : dir, ipconfig, hostname, exit");
            }
            System.out.println();

        while (true) {
                System.out.print("[" + login + "]>> ");
                String commande = scanner.nextLine().trim();

                if (commande.equalsIgnoreCase("exit")) break;
                if (commande.isEmpty()) continue;

                 if (commande.startsWith("UPLOAD "))
                    envoyerFichier(commande.substring(7).trim(), envoi, reception);
                else if (commande.startsWith("DOWNLOAD "))
                    telechargerFichier(commande.substring(9).trim(), envoi, reception);
                else
                    envoyerCommande(commande, envoi, reception);
            }

            System.out.println("Déconnexion.");

        } catch (ConnectException e) {
            System.err.println("[Client] Serveur injoignable. Vérifiez que le serveur est démarré.");
        } catch (IOException e) {
            System.err.println("[Client] Erreur : " + e.getMessage());
        }
    }

 // ════════════════════════════════════════════════════════════════════
    //  Méthodes de traitement des commandes
    // ════════════════════════════════════════════════════════════════════


    private static void envoyerCommande(String commande,
                                         PrintWriter envoi,
                                         BufferedReader reception) throws IOException {
        envoi.println(commande);
        System.out.println("--- Résultat ---");
        String ligne;
        while ((ligne = reception.readLine()) != null) {
            if (ligne.equals(FIN)) break;
            System.out.println(ligne);
        }
        System.out.println("----------------\n");
    }

   
    private static void envoyerFichier(String cheminFichier,
                                        PrintWriter envoi,
                                        BufferedReader reception) throws IOException {
        File fichier = new File(cheminFichier);
        if (!fichier.exists()) {
            System.err.println("[ERREUR] Fichier local introuvable : " + cheminFichier);
            return;
        }
        envoi.println("UPLOAD " + fichier.getName());
        try (BufferedReader fr = new BufferedReader(new FileReader(fichier))) {
            String ligne;
            while ((ligne = fr.readLine()) != null)
                envoi.println(ligne);
        }
        envoi.println(FIN_FICHIER);

        // Lire la confirmation du serveur
        System.out.println("--- Résultat ---");
        String ligne;
        while ((ligne = reception.readLine()) != null) {
            if (ligne.equals(FIN)) break;
            System.out.println(ligne);
        }
        System.out.println("----------------\n");
    }


    private static void telechargerFichier(String nomFichier,
                                            PrintWriter envoi,
                                            BufferedReader reception) throws IOException {
        envoi.println("DOWNLOAD " + nomFichier);
        new File("downloads").mkdirs();
        File fichierLocal = new File("downloads", nomFichier);
        boolean erreur = false;

        try (BufferedWriter bw = new BufferedWriter(new FileWriter(fichierLocal))) {
            String ligne;
            while ((ligne = reception.readLine()) != null) {
                if (ligne.equals(FIN_FICHIER)) break;
                if (ligne.equals(FIN)) { erreur = true; break; }
                bw.write(ligne);
                bw.newLine();
            }
            
            // Si le fichier n'existait pas côté serveur, consomme le FIN résiduel
            if (!erreur) reception.readLine();
        }

        if (!erreur)
            System.out.println("[✓] Fichier sauvegardé dans downloads/" + nomFichier + "\n");
    }
}