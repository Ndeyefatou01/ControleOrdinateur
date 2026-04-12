package client;

import java.io.*;
import java.net.*;
import java.util.Scanner;

public class Client {

    public static void main(String[] args) {

        String adresseServeur = "127.0.0.1";
        int port = 5000;

        System.out.println("=== Client démarré ===");
        System.out.println("Connexion à " + adresseServeur + ":" + port);

        try (
            Socket socket = new Socket(adresseServeur, port);

            PrintWriter envoi = new PrintWriter(
                socket.getOutputStream(), true);

            BufferedReader reception = new BufferedReader(
                new InputStreamReader(socket.getInputStream()));

            Scanner scanner = new Scanner(System.in);
        ) {
            System.out.println("[Client] Connecté au serveur !");
            System.out.println("Tapez une commande (ex: ls ou dir). 'exit' pour quitter.\n");

             //Authentification 
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

            // Récupérer le rôle depuis "AUTH_OK:ADMIN" ou "AUTH_OK:USER"
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

                 // ── UPLOAD
                if (commande.startsWith("UPLOAD ")) {
                    String cheminFichier = commande.substring(7).trim();
                    File fichier = new File(cheminFichier);
                    if (!fichier.exists()) {
                        System.err.println("[ERREUR] Fichier local introuvable : " + cheminFichier);
                        continue;
                    }
                    envoi.println("UPLOAD " + fichier.getName());
                    try (BufferedReader fr = new BufferedReader(new FileReader(fichier))) {
                        String ligne;
                        while ((ligne = fr.readLine()) != null)
                            envoi.println(ligne);
                    }
                    envoi.println("---FIN_FICHIER---");
                    System.out.println("--- Résultat ---");
                    String ligne;
                    while ((ligne = reception.readLine()) != null) {
                        if (ligne.equals("---FIN---")) break;
                        System.out.println(ligne);
                    }
                    System.out.println("----------------\n");

                // ── DOWNLOAD
                } else if (commande.startsWith("DOWNLOAD ")) {
                    String nomFichier = commande.substring(9).trim();
                    envoi.println(commande);
                    new File("downloads").mkdirs();
                    File fichierLocal = new File("downloads", nomFichier);
                    boolean erreur = false;
                    try (BufferedWriter bw = new BufferedWriter(new FileWriter(fichierLocal))) {
                        String ligne;
                        while ((ligne = reception.readLine()) != null) {
                            if (ligne.equals("---FIN_FICHIER---")) break;
                            if (ligne.equals("---FIN---")) { erreur = true; break; }
                            bw.write(ligne);
                            bw.newLine();
                        }
                        if (!erreur) reception.readLine(); 
                    }
                    if (!erreur)
                        System.out.println("[✓] Fichier sauvegardé dans downloads/" + nomFichier + "\n");

                // ── Commande système normale 
                } else {
                envoi.println(commande);
                System.out.println("--- Résultat ---");
                String ligne;
                while ((ligne = reception.readLine()) != null) {
                    if (ligne.equals("---FIN---")) break;
                    System.out.println(ligne);
                }
                System.out.println("----------------\n");
            }
        }
            System.out.println("Déconnexion.");

        } catch (ConnectException e) {
            System.err.println("[Client] Serveur injoignable.");
        } catch (IOException e) {
            System.err.println("[Client] Erreur : " + e.getMessage());
        }
    }
}