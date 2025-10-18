package ar.edu.up.bugtracker.ui;

import ar.edu.up.bugtracker.controller.UserController;
import ar.edu.up.bugtracker.controller.UserRoleController;
import ar.edu.up.bugtracker.service.dto.UserLoggedInDto;
import ar.edu.up.bugtracker.ui.users.auth.LoginPanel;
import ar.edu.up.bugtracker.ui.users.auth.RegisterPanel;
import ar.edu.up.bugtracker.ui.components.HeaderPanel;
import ar.edu.up.bugtracker.ui.projects.HomePanel;
import ar.edu.up.bugtracker.ui.users.MiPerfilPanel;
import ar.edu.up.bugtracker.ui.users.UsuariosListPanel;

import javax.swing.*;
import java.awt.*;

public class PanelManager extends JFrame {

    private final UserController userController;
    private final UserRoleController roleController;

    // Estado de sesión
    private UserLoggedInDto currentUser;

    // Paneles
    private final JPanel root;            // contenedor central (swaps)
    private HeaderPanel headerPanel;      // aparece luego de login

    // Vistas
    private LoginPanel loginPanel;
    private RegisterPanel registerPanel;
    private HomePanel homePanel;
    private MiPerfilPanel miPerfilPanel;
    private UsuariosListPanel usuariosListPanel;

    public PanelManager(UserController userController, UserRoleController roleController) {
        super("BugTracker");
        this.userController = userController;
        this.roleController = roleController;

        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setMinimumSize(new Dimension(900, 600));
        setLocationRelativeTo(null);
        setLayout(new BorderLayout());

        // Contenedor central donde se montan las pantallas
        root = new JPanel(new BorderLayout());
        add(root, BorderLayout.CENTER);

        showLogin();
    }

    // Navegación

    public void showLogin() {
        // Header fuera en login/registro
        if (headerPanel != null) remove(headerPanel);
        loginPanel = new LoginPanel(this, userController);
        swapCenter(loginPanel);
    }

    public void showRegister() {
        if (headerPanel != null) remove(headerPanel);
        registerPanel = new RegisterPanel(this, userController, roleController);
        swapCenter(registerPanel);
    }

    public void onLoginSuccess(UserLoggedInDto user) {
        this.currentUser = user;
        buildHeader();
        showHome();
    }

    public void logout() {
        this.currentUser = null;
        showLogin();
    }

    private void buildHeader() {
        if (headerPanel != null) remove(headerPanel);
        headerPanel = new HeaderPanel(
                this::showHome,
                this::showMiPerfil,
                this::showUsuarios,
                this::logout,
                isAdmin()
        );
        add(headerPanel, BorderLayout.NORTH);
        validate();
        repaint();
    }

    public void showHome() {
        homePanel = new HomePanel();
        swapCenter(homePanel);
    }

    public void showMiPerfil() {
        if (currentUser == null) {
            JOptionPane.showMessageDialog(this, "Sesión expirada. Iniciá sesión nuevamente.");
            showLogin();
            return;
        }
        miPerfilPanel = new MiPerfilPanel(this, userController, currentUser);
        swapCenter(miPerfilPanel);
    }

    public void showUsuarios() {
        if (!isAdmin()) {
            JOptionPane.showMessageDialog(this, "Acceso restringido a administradores.");
            return;
        }
        Long currentId = (currentUser != null ? currentUser.getId() : null);
        usuariosListPanel = new UsuariosListPanel(userController, currentId,roleController);
        swapCenter(usuariosListPanel);
    }

    private void swapCenter(JComponent view) {
        root.removeAll();
        root.add(view, BorderLayout.CENTER);
        root.validate();
        root.repaint();
        setVisible(true);
    }

    public boolean isAdmin() {
        return currentUser != null && "ADMIN".equalsIgnoreCase(currentUser.getPerfil());
    }

    public UserLoggedInDto getCurrentUser() {
        return currentUser;
    }
}
