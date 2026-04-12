package client;

import javax.swing.*;
import javax.swing.border.*;
import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.net.*;


public class ClientGUI extends JFrame {

    // ─── Composants de connexion ────────────────────────────────────────────
    private JTextField champIP;
    private JTextField champPort;
    private JButton boutonConnexion;
    private JLabel labelStatut;

    // ─── Composants de commande ─────────────────────────────────────────────
    private JTextField champCommande;
    private JButton boutonEnvoyer;
    private JTextArea zoneResultat;
    private DefaultListModel<String> historiqueModel;
    private JList<String> listeHistorique;

    // ─── Réseau ─────────────────────────────────────────────────────────────
    private Socket socket;
    private PrintWriter envoi;
    private BufferedReader reception;
    private boolean connecte = false;


    private static final Color COULEUR_FOND       = new Color(30, 30, 46);
    private static final Color COULEUR_PANNEAU    = new Color(49, 50, 68);
    private static final Color COULEUR_ACCENT     = new Color(137, 180, 250);
    private static final Color COULEUR_VERT       = new Color(166, 227, 161);
    private static final Color COULEUR_ROUGE      = new Color(243, 139, 168);
    private static final Color COULEUR_TEXTE      = new Color(205, 214, 244);
    private static final Color COULEUR_SAISIE     = new Color(69, 71, 90);
    private static final Color COULEUR_JAUNE      = new Color(249, 226, 175);

    public ClientGUI() {
        setTitle("Contrôle à Distance — Client");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(850, 620);
        setMinimumSize(new Dimension(700, 500));
        setLocationRelativeTo(null);
        getContentPane().setBackground(COULEUR_FOND);
        setLayout(new BorderLayout(10, 10));

        construireUI();
        setVisible(true);
    }

    // ════════════════════════════════════════════════════════════════════════
    //  Construction de l'interface
    // ════════════════════════════════════════════════════════════════════════

    private void construireUI() {
        add(creerBandeau(), BorderLayout.NORTH);

        JSplitPane splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT,
                creerPanneauHistorique(), creerPanneauPrincipal());
        splitPane.setDividerLocation(200);
        splitPane.setDividerSize(4);
        splitPane.setBorder(null);
        splitPane.setBackground(COULEUR_FOND);
        add(splitPane, BorderLayout.CENTER);
    }

    /** Bandeau supérieur : connexion + statut */
    private JPanel creerBandeau() {
        JPanel bandeau = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 8));
        bandeau.setBackground(COULEUR_PANNEAU);
        bandeau.setBorder(new MatteBorder(0, 0, 2, 0, COULEUR_ACCENT));

       
        JLabel icone = new JLabel("⬛ ");
        icone.setFont(new Font("Monospaced", Font.BOLD, 18));
        icone.setForeground(COULEUR_ACCENT);
        bandeau.add(icone);

        JLabel lblIP = creerLabel("IP :");
        champIP = creerChampTexte("127.0.0.1", 120);

        JLabel lblPort = creerLabel("Port :");
        champPort = creerChampTexte("5000", 60);

        boutonConnexion = creerBouton("Connecter", COULEUR_VERT);
        boutonConnexion.addActionListener(e -> basculerConnexion());

        labelStatut = new JLabel("● Déconnecté");
        labelStatut.setForeground(COULEUR_ROUGE);
        labelStatut.setFont(new Font("Monospaced", Font.BOLD, 13));

        bandeau.add(lblIP);
        bandeau.add(champIP);
        bandeau.add(lblPort);
        bandeau.add(champPort);
        bandeau.add(boutonConnexion);
        bandeau.add(Box.createHorizontalStrut(20));
        bandeau.add(labelStatut);

        return bandeau;
    }

    /** Panneau gauche : historique des commandes */
    private JPanel creerPanneauHistorique() {
        JPanel panneau = new JPanel(new BorderLayout(0, 5));
        panneau.setBackground(COULEUR_PANNEAU);
        panneau.setBorder(new EmptyBorder(8, 8, 8, 4));

        JLabel titre = creerLabel("📋 Historique");
        titre.setFont(new Font("Monospaced", Font.BOLD, 13));
        panneau.add(titre, BorderLayout.NORTH);

        historiqueModel = new DefaultListModel<>();
        listeHistorique = new JList<>(historiqueModel);
        listeHistorique.setBackground(COULEUR_SAISIE);
        listeHistorique.setForeground(COULEUR_TEXTE);
        listeHistorique.setFont(new Font("Monospaced", Font.PLAIN, 12));
        listeHistorique.setSelectionBackground(COULEUR_ACCENT);
        listeHistorique.setSelectionForeground(COULEUR_FOND);
        listeHistorique.setBorder(new EmptyBorder(4, 4, 4, 4));

        // Double-clic = renvoyer la commande dans le champ
        listeHistorique.addMouseListener(new MouseAdapter() {
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2) {
                    String sel = listeHistorique.getSelectedValue();
                    if (sel != null) champCommande.setText(sel);
                }
            }
        });

        JScrollPane scroll = new JScrollPane(listeHistorique);
        scroll.setBorder(BorderFactory.createLineBorder(COULEUR_ACCENT, 1));
        scroll.setBackground(COULEUR_SAISIE);
        panneau.add(scroll, BorderLayout.CENTER);

        JButton boutonVider = creerBouton("Vider", COULEUR_ROUGE);
        boutonVider.setFont(new Font("Monospaced", Font.PLAIN, 11));
        boutonVider.addActionListener(e -> historiqueModel.clear());
        panneau.add(boutonVider, BorderLayout.SOUTH);

        return panneau;
    }

    /** Panneau principal : zone résultat + champ de saisie */
    private JPanel creerPanneauPrincipal() {
        JPanel panneau = new JPanel(new BorderLayout(0, 8));
        panneau.setBackground(COULEUR_FOND);
        panneau.setBorder(new EmptyBorder(8, 4, 8, 8));

        // Zone de résultat (terminal style)
        zoneResultat = new JTextArea();
        zoneResultat.setEditable(false);
        zoneResultat.setBackground(new Color(17, 17, 27));
        zoneResultat.setForeground(COULEUR_VERT);
        zoneResultat.setFont(new Font("Monospaced", Font.PLAIN, 13));
        zoneResultat.setBorder(new EmptyBorder(8, 10, 8, 10));
        zoneResultat.setText("╔══════════════════════════════════════╗\n" +
                             "║  Client de Contrôle à Distance       ║\n" +
                             "║  Connectez-vous puis envoyez des      ║\n" +
                             "║  commandes au serveur distant.        ║\n" +
                             "╚══════════════════════════════════════╝\n");

        JScrollPane scrollResultat = new JScrollPane(zoneResultat);
        scrollResultat.setBorder(BorderFactory.createLineBorder(COULEUR_ACCENT, 1));
        panneau.add(scrollResultat, BorderLayout.CENTER);

        // Barre de saisie bas
        panneau.add(creerBarreSaisie(), BorderLayout.SOUTH);

        return panneau;
    }

    /** Barre de saisie : champ commande + bouton envoyer + bouton effacer */
    private JPanel creerBarreSaisie() {
        JPanel barre = new JPanel(new BorderLayout(6, 0));
        barre.setBackground(COULEUR_FOND);

        JLabel prompt = new JLabel("$ ");
        prompt.setForeground(COULEUR_VERT);
        prompt.setFont(new Font("Monospaced", Font.BOLD, 15));
        barre.add(prompt, BorderLayout.WEST);

        champCommande = new JTextField();
        champCommande.setBackground(COULEUR_SAISIE);
        champCommande.setForeground(COULEUR_TEXTE);
        champCommande.setCaretColor(COULEUR_TEXTE);
        champCommande.setFont(new Font("Monospaced", Font.PLAIN, 13));
        champCommande.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(COULEUR_ACCENT, 1),
                new EmptyBorder(4, 8, 4, 8)));
        champCommande.setEnabled(false);
        champCommande.addActionListener(e -> envoyerCommande());
        barre.add(champCommande, BorderLayout.CENTER);

        JPanel boutons = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 0));
        boutons.setBackground(COULEUR_FOND);

        boutonEnvoyer = creerBouton("Envoyer ▶", COULEUR_ACCENT);
        boutonEnvoyer.setForeground(COULEUR_FOND);
        boutonEnvoyer.setEnabled(false);
        boutonEnvoyer.addActionListener(e -> envoyerCommande());

        JButton boutonEffacer = creerBouton("Effacer", COULEUR_SAISIE);
        boutonEffacer.addActionListener(e -> {
            zoneResultat.setText("");
            champCommande.requestFocus();
        });

        JButton boutonUpload = creerBouton("📁 Upload", COULEUR_JAUNE);
        boutonUpload.setForeground(COULEUR_FOND);
        boutonUpload.addActionListener(e -> ouvrirSelecteurFichier());

        boutons.add(boutonEnvoyer);
        boutons.add(boutonUpload);
        boutons.add(boutonEffacer);
        barre.add(boutons, BorderLayout.EAST);

        return barre;
    }

    // ════════════════════════════════════════════════════════════════════════
    //  Logique réseau
    // ════════════════════════════════════════════════════════════════════════

    /** Connecte ou déconnecte selon l'état actuel */
    private void basculerConnexion() {
        if (connecte) {
            seDeconnecter();
        } else {
            seConnecter();
        }
    }

    private void seConnecter() {
        String ip   = champIP.getText().trim();
        String portStr = champPort.getText().trim();

        if (ip.isEmpty() || portStr.isEmpty()) {
            afficherErreur("Veuillez renseigner l'IP et le port.");
            return;
        }

        int port;
        try {
            port = Integer.parseInt(portStr);
        } catch (NumberFormatException e) {
            afficherErreur("Port invalide.");
            return;
        }

        boutonConnexion.setEnabled(false);
        labelStatut.setText("● Connexion en cours...");
        labelStatut.setForeground(COULEUR_ACCENT);

        // Connexion dans un thread séparé pour ne pas bloquer l'UI
        new Thread(() -> {
            try {
                socket    = new Socket(ip, port);
                envoi     = new PrintWriter(socket.getOutputStream(), true);
                reception = new BufferedReader(new InputStreamReader(socket.getInputStream()));

                String msg = reception.readLine();
                if (!"LOGIN_REQUIRED".equals(msg)) {
                    throw new IOException("Protocole inattendu : " + msg);
                }

                String[] identifiants = new String[2];
                java.util.concurrent.CountDownLatch latch =
                        new java.util.concurrent.CountDownLatch(1);

                SwingUtilities.invokeLater(() -> {
                    JPanel panel = new JPanel(new GridLayout(2, 2, 5, 8));
                    panel.setBackground(COULEUR_FOND);
                    JLabel lblLogin = new JLabel("Login :");
                    lblLogin.setForeground(COULEUR_TEXTE);
                    JLabel lblMdp = new JLabel("Mot de passe :");
                    lblMdp.setForeground(COULEUR_TEXTE);
                    JTextField champLogin = new JTextField();
                    JPasswordField champMdp = new JPasswordField();
                    panel.add(lblLogin);  panel.add(champLogin);
                    panel.add(lblMdp);    panel.add(champMdp);

                    int result = JOptionPane.showConfirmDialog(
                            ClientGUI.this, panel, "Authentification",
                            JOptionPane.OK_CANCEL_OPTION,
                            JOptionPane.PLAIN_MESSAGE);

                    if (result == JOptionPane.OK_OPTION) {
                        identifiants[0] = champLogin.getText().trim();
                        identifiants[1] = new String(champMdp.getPassword());
                    }
                    latch.countDown();
                });

                latch.await();

                // Annulation par l'utilisateur
                if (identifiants[0] == null || identifiants[0].isEmpty()) {
                    socket.close();
                    SwingUtilities.invokeLater(() -> {
                        labelStatut.setText("● Déconnecté");
                        labelStatut.setForeground(COULEUR_ROUGE);
                        boutonConnexion.setEnabled(true);
                    });
                    return;
                }

                // Envoi des identifiants
                envoi.println(identifiants[0]);
                envoi.println(identifiants[1]);

                String reponse = reception.readLine();
                if (reponse == null || reponse.equals("AUTH_FAIL")) {
                    socket.close();
                    SwingUtilities.invokeLater(() -> {
                        labelStatut.setText("● Déconnecté");
                        labelStatut.setForeground(COULEUR_ROUGE);
                        boutonConnexion.setEnabled(true);
                        afficherErreur("Identifiants incorrects.");
                    });
                    return;
                }

                String role = reponse.contains(":") ? reponse.split(":")[1] : "USER";
                boolean estAdmin = "ADMIN".equals(role);
                connecte = true;
                final String loginFinal = identifiants[0];

                SwingUtilities.invokeLater(() -> {
                    labelStatut.setText("● " + loginFinal + " [" + role + "] connecté");
                    labelStatut.setForeground(COULEUR_VERT);
                    boutonConnexion.setText("Déconnecter");
                    boutonConnexion.setBackground(COULEUR_ROUGE);
                    boutonConnexion.setEnabled(true);
                    champCommande.setEnabled(true);
                    boutonEnvoyer.setEnabled(true);
                    champIP.setEnabled(false);
                    champPort.setEnabled(false);
                    champCommande.requestFocus();
                    ajouterLog("\n[✓] Connecté en tant que " + loginFinal + " [" + role + "]\n");
                    if (estAdmin)
                        ajouterLog("Commande admin disponible : SIGNUP login:mdp\n");
                });

            } catch (InterruptedException ex) {
                Thread.currentThread().interrupt();
            } catch (IOException ex) {
                connecte = false;
                SwingUtilities.invokeLater(() -> {
                    labelStatut.setText("● Déconnecté");
                    labelStatut.setForeground(COULEUR_ROUGE);
                    boutonConnexion.setEnabled(true);
                    afficherErreur("Connexion impossible : " + ex.getMessage());
                });
            }
        }).start();
    }

    private void seDeconnecter() {
        try {
            if (socket != null && !socket.isClosed()) socket.close();
        } catch (IOException ignored) {}
        connecte = false;
        labelStatut.setText("● Déconnecté");
        labelStatut.setForeground(COULEUR_ROUGE);
        boutonConnexion.setText("Connecter");
        boutonConnexion.setBackground(COULEUR_VERT);
        champCommande.setEnabled(false);
        boutonEnvoyer.setEnabled(false);
        champIP.setEnabled(true);
        champPort.setEnabled(true);
        ajouterLog("\n[✗] Déconnecté du serveur.\n");
    }

    private void envoyerCommande() {
        String commande = champCommande.getText().trim();
        if (commande.isEmpty() || !connecte) return;

        if (historiqueModel.isEmpty() ||
                !historiqueModel.get(historiqueModel.size() - 1).equals(commande)) {
            historiqueModel.addElement(commande);
        }
        champCommande.setText("");
        boutonEnvoyer.setEnabled(false);
        ajouterLog("\n$ " + commande + "\n");

        new Thread(() -> {
            try {

            if (commande.startsWith("UPLOAD ")) {
                String cheminFichier = commande.substring(7).trim();
                File fichier = new File(cheminFichier);
                if (!fichier.exists()) {
                    SwingUtilities.invokeLater(() -> {
                        ajouterLog("[ERREUR] Fichier introuvable : " + cheminFichier + "\n");
                        boutonEnvoyer.setEnabled(true);
                    });
                    return;
                }
                envoi.println("UPLOAD " + fichier.getName());
                try (BufferedReader fr = new BufferedReader(new FileReader(fichier))) {
                    String ligne;
                    while ((ligne = fr.readLine()) != null)
                        envoi.println(ligne);
                }
                envoi.println("---FIN_FICHIER---");
                // Lire la confirmation du serveur
                StringBuilder sb = new StringBuilder();
                String ligne;
                while ((ligne = reception.readLine()) != null) {
                    if (ligne.equals("---FIN---")) break;
                    sb.append(ligne).append("\n");
                }
                final String resultat = sb.toString();
                SwingUtilities.invokeLater(() -> {
                    ajouterLog(resultat.isEmpty() ? "(pas de réponse)\n" : resultat);
                    boutonEnvoyer.setEnabled(true);
                    champCommande.requestFocus();
                });

            } else if (commande.startsWith("DOWNLOAD ")) {
            String nomFichier = commande.substring(9).trim();
            envoi.println(commande);
            new File("downloads").mkdirs();
            File fichierLocal = new File("downloads", nomFichier);
            StringBuilder sbErreur = new StringBuilder();
            boolean erreur = false;
            try (BufferedWriter bw = new BufferedWriter(new FileWriter(fichierLocal))) {
                String ligne;
                while ((ligne = reception.readLine()) != null) {
                    if (ligne.equals("---FIN_FICHIER---")) break;
                    if (ligne.startsWith("[ERREUR]")) {
                        sbErreur.append(ligne);
                        erreur = true;
                        reception.readLine(); 
                        break;
                    }
                    bw.write(ligne);
                    bw.newLine();
                }
            }
            final boolean echecFinal = erreur;
            final String nomFinal = nomFichier;
            final String msgErreur = sbErreur.toString();
            SwingUtilities.invokeLater(() -> {
                if (echecFinal)
                    ajouterLog(msgErreur + "\n");
                else
                    ajouterLog("[✓] Fichier sauvegardé dans downloads/" + nomFinal + "\n");
                boutonEnvoyer.setEnabled(true);
                champCommande.requestFocus();
            });

              } else {
                envoi.println(commande);
                StringBuilder sb = new StringBuilder();
                String ligne;
                while ((ligne = reception.readLine()) != null) {
                    if (ligne.equals("---FIN---")) break;
                    sb.append(ligne).append("\n");
                }
                String resultat = sb.toString();
                SwingUtilities.invokeLater(() -> {
                    ajouterLog(resultat.isEmpty() ? "(pas de sortie)\n" : resultat);
                    boutonEnvoyer.setEnabled(true);
                    champCommande.requestFocus();
                });
              }
            } catch (IOException ex) {
                SwingUtilities.invokeLater(() -> {
                    ajouterLog("[ERREUR] " + ex.getMessage() + "\n");
                    seDeconnecter();
                });
            }
        }).start();
    }
    private void ouvrirSelecteurFichier() {
    if (!connecte) {
        afficherErreur("Connectez-vous d'abord au serveur.");
        return;
    }
    JFileChooser chooser = new JFileChooser();
    chooser.setDialogTitle("Choisir un fichier à uploader");
    int resultat = chooser.showOpenDialog(this);
    if (resultat == JFileChooser.APPROVE_OPTION) {
        File fichier = chooser.getSelectedFile();
        champCommande.setText("UPLOAD " + fichier.getAbsolutePath());
        envoyerCommande();
    }
}

    // ════════════════════════════════════════════════════════════════════════
    //  Utilitaires
    // ════════════════════════════════════════════════════════════════════════

    private void ajouterLog(String texte) {
        zoneResultat.append(texte);
        zoneResultat.setCaretPosition(zoneResultat.getDocument().getLength());
    }

    private void afficherErreur(String msg) {
        JOptionPane.showMessageDialog(this, msg, "Erreur", JOptionPane.ERROR_MESSAGE);
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
                BorderFactory.createLineBorder(COULEUR_ACCENT, 1),
                new EmptyBorder(2, 6, 2, 6)));
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

    // ════════════════════════════════════════════════════════════════════════
    //  Point d'entrée
    // ════════════════════════════════════════════════════════════════════════

    public static void main(String[] args) {
        try {
            UIManager.setLookAndFeel(UIManager.getCrossPlatformLookAndFeelClassName());
        } catch (Exception ignored) {}

        SwingUtilities.invokeLater(ClientGUI::new);
    }
}