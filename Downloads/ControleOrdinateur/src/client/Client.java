package client;

import java.io.*;
import java.net.*;
import java.util.Scanner;

public class Client {

    public static void main(String[] args) {
        // Adresse IP du serveur (localhost = même machine pour les tests)
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

            while (true) {
                System.out.print(">> ");
                String commande = scanner.nextLine();

                if (commande.equalsIgnoreCase("exit")) {
                    System.out.println("Déconnexion...");
                    break;
                }

                // Envoyer la commande au serveur
                envoi.println(commande);

                // Lire et afficher la réponse ligne par ligne
                System.out.println("--- Résultat ---");
                String ligne;
                while ((ligne = reception.readLine()) != null) {
                    if (ligne.equals("---FIN---")) break; // Signal de fin
                    System.out.println(ligne);
                }
                System.out.println("----------------\n");
            }

        } catch (ConnectException e) {
            System.err.println("[Client] Impossible de se connecter. Le serveur est-il démarré ?");
        } catch (IOException e) {
            System.err.println("[Client] Erreur : " + e.getMessage());
        }
    }
}