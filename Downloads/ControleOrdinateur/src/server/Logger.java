package server;

import java.io.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class Logger {

    private static final String FICHIER_LOG = "server.log";
    private static final DateTimeFormatter FMT =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private Logger() {}

    public static synchronized void log(String niveau, String message) {
        String ligne = "[" + LocalDateTime.now().format(FMT) + "] "
                     + "[" + niveau + "] " + message;
        System.out.println(ligne);
        try (FileWriter fw = new FileWriter(FICHIER_LOG, true);
             BufferedWriter bw = new BufferedWriter(fw)) {
            bw.write(ligne);
            bw.newLine();
        } catch (IOException e) {
            System.err.println("[Logger] Erreur écriture : " + e.getMessage());
        }
    }

    public static void info(String msg)   { log("INFO",   msg); }
    public static void warn(String msg)   { log("WARN",   msg); }
    public static void erreur(String msg) { log("ERREUR", msg); }
}