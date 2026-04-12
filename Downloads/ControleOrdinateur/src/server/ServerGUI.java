package server;

import javax.swing.*;
import javax.swing.border.*;
import javax.swing.table.*;
import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.net.*;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.atomic.AtomicInteger;

public class ServerGUI extends JFrame {

    private static final int PORT_DEFAUT = 5000;
    private ServerSocket serverSocket;
    private volatile boolean actif = false;
    private final AtomicInteger compteurClients = new AtomicInteger(0);

    private JButton boutonDemarrer;
    private JLabel labelStatut;
    private JTextField champPort;
    private DefaultTableModel tableModel;
    private JTable tableClients;
    private JTextArea zoneJournal;
    private AuthManager auth;

    private static final Color COULEUR_FOND    = new Color(30, 30, 46);
    private static final Color COULEUR_PANNEAU = new Color(49, 50, 68);
    private static final Color COULEUR_ACCENT  = new Color(137, 180, 250);
    private static final Color COULEUR_VERT    = new Color(166, 227, 161);
    private static final Color COULEUR_ROUGE   = new Color(243, 139, 168);
    private static final Color COULEUR_JAUNE   = new Color(249, 226, 175);
    private static final Color COULEUR_TEXTE   = new Color(205, 214, 244);
    private static final Color COULEUR_SAISIE  = new Color(69, 71, 90);

    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("HH:mm:ss");

    public ServerGUI() {
        setTitle("Controle a Distance — Serveur");
        setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
        addWindowListener(new WindowAdapter() {
            public void windowClosing(WindowEvent e) { arreterServeur(); System.exit(0); }
        });
        setSize(880, 600);
        setMinimumSize(new Dimension(700, 450));
        setLocationRelativeTo(null);
        getContentPane().setBackground(COULEUR_FOND);
        setLayout(new BorderLayout(10, 10));
        construireUI();
        setVisible(true);
    }

    private void construireUI() {
        add(creerBandeau(), BorderLayout.NORTH);
        JSplitPane split = new JSplitPane(JSplitPane.VERTICAL_SPLIT,
                creerPanneauClients(), creerPanneauJournal());
        split.setDividerLocation(220);
        split.setDividerSize(4);
        split.setBorder(null);
        split.setBackground(COULEUR_FOND);
        add(split, BorderLayout.CENTER);
    }

    private JPanel creerBandeau() {
        JPanel bandeau = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 8));
        bandeau.setBackground(COULEUR_PANNEAU);
        bandeau.setBorder(new MatteBorder(0, 0, 2, 0, COULEUR_ACCENT));
        JLabel lblPort = creerLabel("Port :");
        champPort = creerChampTexte(String.valueOf(PORT_DEFAUT), 70);
        boutonDemarrer = creerBouton("Demarrer", COULEUR_VERT);
        boutonDemarrer.addActionListener(e -> basculerServeur());
        labelStatut = new JLabel("● Arrete");
        labelStatut.setForeground(COULEUR_ROUGE);
        labelStatut.setFont(new Font("Monospaced", Font.BOLD, 13));
        bandeau.add(lblPort);
        bandeau.add(champPort);
        bandeau.add(boutonDemarrer);
        bandeau.add(Box.createHorizontalStrut(20));
        bandeau.add(labelStatut);
        return bandeau;
    }

    private JPanel creerPanneauClients() {
        JPanel panneau = new JPanel(new BorderLayout(0, 5));
        panneau.setBackground(COULEUR_PANNEAU);
        panneau.setBorder(new EmptyBorder(8, 8, 8, 8));
        JLabel titre = creerLabel("Clients connectes");
        titre.setFont(new Font("Monospaced", Font.BOLD, 13));
        panneau.add(titre, BorderLayout.NORTH);
        String[] colonnes = {"#", "Adresse IP", "Heure connexion", "Commandes", "Statut"};
        tableModel = new DefaultTableModel(colonnes, 0) {
            public boolean isCellEditable(int r, int c) { return false; }
        };
        tableClients = new JTable(tableModel);
        tableClients.setBackground(COULEUR_SAISIE);
        tableClients.setForeground(COULEUR_TEXTE);
        tableClients.setFont(new Font("Monospaced", Font.PLAIN, 12));
        tableClients.setRowHeight(22);
        tableClients.setGridColor(COULEUR_PANNEAU);
        tableClients.setSelectionBackground(COULEUR_ACCENT);
        tableClients.setSelectionForeground(COULEUR_FOND);
        tableClients.setShowVerticalLines(false);
        JTableHeader header = tableClients.getTableHeader();
        header.setBackground(COULEUR_FOND);
        header.setForeground(COULEUR_ACCENT);
        header.setFont(new Font("Monospaced", Font.BOLD, 12));
        tableClients.getColumnModel().getColumn(0).setMaxWidth(40);
        tableClients.getColumnModel().getColumn(3).setMaxWidth(140);
        tableClients.getColumnModel().getColumn(4).setMaxWidth(100);
        JScrollPane scroll = new JScrollPane(tableClients);
        scroll.setBorder(BorderFactory.createLineBorder(COULEUR_ACCENT, 1));
        scroll.getViewport().setBackground(COULEUR_SAISIE);
        panneau.add(scroll, BorderLayout.CENTER);
        return panneau;
    }

    private JPanel creerPanneauJournal() {
        JPanel panneau = new JPanel(new BorderLayout(0, 5));
        panneau.setBackground(COULEUR_FOND);
        panneau.setBorder(new EmptyBorder(4, 8, 8, 8));
        JPanel barTitre = new JPanel(new BorderLayout());
        barTitre.setBackground(COULEUR_FOND);
        JLabel titre = creerLabel("Journal du serveur");
        titre.setFont(new Font("Monospaced", Font.BOLD, 13));
        barTitre.add(titre, BorderLayout.WEST);
        JButton boutonVider = creerBouton("Vider", COULEUR_SAISIE);
        boutonVider.setFont(new Font("Monospaced", Font.PLAIN, 11));
        boutonVider.addActionListener(e -> zoneJournal.setText(""));
        barTitre.add(boutonVider, BorderLayout.EAST);
        panneau.add(barTitre, BorderLayout.NORTH);
        zoneJournal = new JTextArea();
        zoneJournal.setEditable(false);
        zoneJournal.setBackground(new Color(17, 17, 27));
        zoneJournal.setForeground(COULEUR_TEXTE);
        zoneJournal.setFont(new Font("Monospaced", Font.PLAIN, 12));
        zoneJournal.setBorder(new EmptyBorder(6, 10, 6, 10));
        JScrollPane scroll = new JScrollPane(zoneJournal);
        scroll.setBorder(BorderFactory.createLineBorder(COULEUR_ACCENT, 1));
        panneau.add(scroll, BorderLayout.CENTER);
        return panneau;
    }

    private void basculerServeur() {
        if (actif) arreterServeur(); else demarrerServeur();
    }

    private void demarrerServeur() {
        int port;
        try { port = Integer.parseInt(champPort.getText().trim()); }
        catch (NumberFormatException e) { JOptionPane.showMessageDialog(this, "Port invalide."); return; }
        auth = new AuthManager();
        new Thread(() -> {
            try {
                serverSocket = new ServerSocket(port);
                actif = true;
                SwingUtilities.invokeLater(() -> {
                    boutonDemarrer.setText("Arreter");
                    boutonDemarrer.setBackground(COULEUR_ROUGE);
                    champPort.setEnabled(false);
                    labelStatut.setText("● En ecoute sur le port " + port);
                    labelStatut.setForeground(COULEUR_VERT);
                    log("[SERVEUR] Demarre sur le port " + port);
                });
                while (actif) {
                    try {
                        Socket clientSocket = serverSocket.accept();
                        int id = compteurClients.incrementAndGet();
                        String ip = clientSocket.getInetAddress().getHostAddress();
                        String heure = LocalTime.now().format(FMT);
                        SwingUtilities.invokeLater(() -> {
                            tableModel.addRow(new Object[]{id, ip, heure, 0, "Connecte"});
                            log("[CLIENT #" + id + "] Connecte depuis " + ip);
                        });
                        Thread t = new Thread(new ClientHandler(clientSocket, auth));
                        t.setName("Client-" + ip);
                        t.setDaemon(true);
                        t.start();
                    } catch (IOException e) {
                        if (actif) SwingUtilities.invokeLater(() -> log("[ERREUR] " + e.getMessage()));
                    }
                }
            } catch (IOException ex) {
                SwingUtilities.invokeLater(() -> log("[ERREUR] Impossible de demarrer : " + ex.getMessage()));
            }
        }).start();
    }

    private void arreterServeur() {
        actif = false;
        try { if (serverSocket != null && !serverSocket.isClosed()) serverSocket.close(); } catch (IOException ignored) {}
        SwingUtilities.invokeLater(() -> {
            boutonDemarrer.setText("Demarrer");
            boutonDemarrer.setBackground(COULEUR_VERT);
            champPort.setEnabled(true);
            labelStatut.setText("● Arrete");
            labelStatut.setForeground(COULEUR_ROUGE);
            for (int i = 0; i < tableModel.getRowCount(); i++)
                if ("Connecte".equals(tableModel.getValueAt(i, 4)))
                    tableModel.setValueAt("Deconnecte", i, 4);
            log("[SERVEUR] Arrete.");
        });
    }

    private void gererClient(Socket socket, int id, int ligne) {
        String ip = socket.getInetAddress().getHostAddress();
        int[] nb = {0};
        try (
            BufferedReader entree = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            PrintWriter sortie = new PrintWriter(socket.getOutputStream(), true)
        ) {
            String commande;
            while ((commande = entree.readLine()) != null) {
                nb[0]++;
                final String cmd = commande;
                final int n = nb[0];
                SwingUtilities.invokeLater(() -> {
                    tableModel.setValueAt(n, ligne, 3);
                    log("[CLIENT #" + id + " | " + ip + "] Commande : " + cmd);
                });
                String resultat = executerCommande(commande);
                sortie.println(resultat);
                sortie.println("---FIN---");
            }
        } catch (IOException e) {
            if (actif) SwingUtilities.invokeLater(() -> log("[ERREUR client #" + id + "] " + e.getMessage()));
        }
        SwingUtilities.invokeLater(() -> {
            if (ligne < tableModel.getRowCount()) tableModel.setValueAt("Deconnecte", ligne, 4);
            log("[CLIENT #" + id + "] Deconnecte.");
        });
        try { socket.close(); } catch (IOException ignored) {}
    }

    private String executerCommande(String commande) {
        StringBuilder resultat = new StringBuilder();
        try {
            String os = System.getProperty("os.name").toLowerCase();
            ProcessBuilder pb;
            if (os.contains("win")) pb = new ProcessBuilder("cmd.exe", "/c", commande);
            else pb = new ProcessBuilder("/bin/sh", "-c", commande);
            pb.redirectErrorStream(true);
            Process process = pb.start();
            BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
            String ligne;
            while ((ligne = reader.readLine()) != null) resultat.append(ligne).append("\n");
            process.waitFor();
        } catch (Exception e) { resultat.append("[ERREUR] ").append(e.getMessage()); }
        return resultat.toString().trim();
    }

    private void log(String message) {
        zoneJournal.append("[" + LocalTime.now().format(FMT) + "] " + message + "\n");
        zoneJournal.setCaretPosition(zoneJournal.getDocument().getLength());
    }

    private JLabel creerLabel(String texte) {
        JLabel lbl = new JLabel(texte);
        lbl.setForeground(COULEUR_TEXTE);
        lbl.setFont(new Font("Monospaced", Font.PLAIN, 13));
        return lbl;
    }

    private JTextField creerChampTexte(String defaut, int largeur) {
        JTextField champ = new JTextField(defaut);
        champ.setPreferredSize(new Dimension(largeur, 28));
        champ.setBackground(COULEUR_SAISIE);
        champ.setForeground(COULEUR_TEXTE);
        champ.setCaretColor(COULEUR_TEXTE);
        champ.setFont(new Font("Monospaced", Font.PLAIN, 13));
        champ.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(COULEUR_ACCENT, 1), new EmptyBorder(2, 6, 2, 6)));
        return champ;
    }

    private JButton creerBouton(String texte, Color fond) {
        JButton btn = new JButton(texte);
        btn.setBackground(fond);
        btn.setForeground(COULEUR_FOND);
        btn.setFocusPainted(false);
        btn.setBorderPainted(false);
        btn.setFont(new Font("Monospaced", Font.BOLD, 12));
        btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        btn.setBorder(new EmptyBorder(5, 12, 5, 12));
        return btn;
    }

    public static void main(String[] args) {
        try { UIManager.setLookAndFeel(UIManager.getCrossPlatformLookAndFeelClassName()); } catch (Exception ignored) {}
        SwingUtilities.invokeLater(ServerGUI::new);
    }
}