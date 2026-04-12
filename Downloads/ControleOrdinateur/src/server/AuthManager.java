package server;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.*;


public class AuthManager {

    private static final String FICHIER_USERS = "users.txt";

    private final Map<String, String[]> comptes = new HashMap<>();

    public AuthManager() {
        chargerComptes();
    }

    private void chargerComptes() {
        File f = new File(FICHIER_USERS);
        if (!f.exists()) {
            ajouterCompte("admin", "admin123", "ADMIN");
            ajouterCompte("user1", "pass1",    "USER");
            sauvegarder();
            Logger.info("[Auth] users.txt créé avec les comptes par défaut.");
        } else {
            try (BufferedReader br = new BufferedReader(new FileReader(f))) {
                String ligne;
                while ((ligne = br.readLine()) != null) {
                    ligne = ligne.trim();
                    if (ligne.isEmpty() || ligne.startsWith("#")) continue;
                    String[] parts = ligne.split(":", 3);
                    if (parts.length < 3) continue;
                    comptes.put(parts[0].trim(),
                                new String[]{parts[1].trim(), parts[2].trim()});
                }
                Logger.info("[Auth] " + comptes.size() + " compte(s) chargé(s).");
            } catch (IOException e) {
                Logger.erreur("[Auth] Erreur lecture : " + e.getMessage());
            }
        }
    }
 
    public boolean authentifier(String login, String motDePasse) {
        String[] data = comptes.get(login);
        if (data == null) return false;
        return data[0].equals(hacher(motDePasse));
    }

    // Rôle 
    public boolean estAdmin(String login) {
        String[] data = comptes.get(login);
        return data != null && "ADMIN".equals(data[1]);
    }

    // Création de compte 
    public synchronized boolean ajouterCompte(String login, String motDePasse, String role) {
        if (comptes.containsKey(login)) return false;
        comptes.put(login, new String[]{hacher(motDePasse), role});
        sauvegarder();
        Logger.info("[Auth] Compte créé : " + login + " (" + role + ")");
        return true;
    }

    public boolean loginExiste(String login) {
        return comptes.containsKey(login);
    }

    // Sauvegarde 
    private synchronized void sauvegarder() {
        try (BufferedWriter bw = new BufferedWriter(new FileWriter(FICHIER_USERS))) {
            bw.write("# format : login:hash_sha256:role");
            bw.newLine();
            for (Map.Entry<String, String[]> e : comptes.entrySet()) {
                bw.write(e.getKey() + ":" + e.getValue()[0] + ":" + e.getValue()[1]);
                bw.newLine();
            }
        } catch (IOException e) {
            Logger.erreur("[Auth] Erreur sauvegarde : " + e.getMessage());
        }
    }

    public static String hacher(String texte) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(texte.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder();
            for (byte b : hash)
                sb.append(String.format("%02x", b));
            return sb.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("SHA-256 non disponible", e);
        }
    }
}