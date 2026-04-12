package server;

import java.io.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.function.Consumer;

public class Logger {

    private static final String FICHIER_LOG = "server.log";
    private static final DateTimeFormatter FMT =
    DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private static Consumer<String> guiListener = null;

    private Logger() {}
       public static void setGuiListener(Consumer<String> listener) {
        guiListener = listener;
    }
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
                if (guiListener != null) {
            final String msg = ligne;
            javax.swing.SwingUtilities.invokeLater(() -> guiListener.accept(msg));
        }
    }

    public static void info(String msg)   { log("INFO",   msg); }
    public static void warn(String msg)   { log("WARN",   msg); }
    public static void erreur(String msg) { log("ERREUR", msg); }
}