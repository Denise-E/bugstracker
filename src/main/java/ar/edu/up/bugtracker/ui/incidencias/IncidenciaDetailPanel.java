package ar.edu.up.bugtracker.ui.incidencias;

import ar.edu.up.bugtracker.controller.ComentarioController;
import ar.edu.up.bugtracker.controller.IncidenciaController;
import ar.edu.up.bugtracker.controller.UserController;
import ar.edu.up.bugtracker.exceptions.NotFoundException;
import ar.edu.up.bugtracker.models.Comentario;
import ar.edu.up.bugtracker.models.Incidencia;
import ar.edu.up.bugtracker.models.IncidenciaEstado;
import ar.edu.up.bugtracker.models.IncidenciaVersion;
import ar.edu.up.bugtracker.models.Usuario;
import ar.edu.up.bugtracker.service.dto.UserDetailDto;
import ar.edu.up.bugtracker.service.dto.UserLoggedInDto;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.border.TitledBorder;
import java.awt.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class IncidenciaDetailPanel extends JPanel {

    private final IncidenciaController incidenciaController;
    private final ComentarioController comentarioController;
    private final UserController userController;
    private final UserLoggedInDto currentUser;
    private final Long incidenciaId;
    private final Runnable onVolver;

    private JPanel mainContentPanel;
    private JPanel sidebarPanel;
    private JPanel historialPanel;
    private Incidencia incidenciaActual;
    private JComboBox<UserDetailDto> comboResponsable;
    private JComboBox<IncidenciaEstado> comboEstado;
    private boolean cargandoEstados = false; 
    private boolean cargandoUsuarios = false;
    private JTextField descripcionField;
    private JButton btnGuardarDescripcion;
    private String descripcionOriginal;
    private JTextArea comentarioTextArea;
    private JButton btnEnviarComentario; 

    public IncidenciaDetailPanel(IncidenciaController incidenciaController,
                                ComentarioController comentarioController,
                                UserController userController,
                                UserLoggedInDto currentUser,
                                Long incidenciaId,
                                Runnable onVolver) {
        this.incidenciaController = incidenciaController;
        this.comentarioController = comentarioController;
        this.userController = userController;
        this.currentUser = currentUser;
        this.incidenciaId = incidenciaId;
        this.onVolver = onVolver;
        buildUI();
        loadIncidencia();
    }

    private void buildUI() {
        setLayout(new BorderLayout());
        setBorder(new EmptyBorder(12, 12, 12, 12));

        JPanel topPanel = new JPanel(new BorderLayout());
        JButton btnVolver = new JButton("← Volver");
        btnVolver.addActionListener(e -> {
            if (onVolver != null) {
                onVolver.run();
            }
        });
        topPanel.add(btnVolver, BorderLayout.WEST);
        add(topPanel, BorderLayout.NORTH);

        JSplitPane splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT);
        splitPane.setDividerLocation(0.8); // 80% izquierda, 20% derecha
        splitPane.setResizeWeight(0.8);
        splitPane.setDividerSize(5);

        // Panel izquierdo
        mainContentPanel = new JPanel();
        mainContentPanel.setLayout(new BorderLayout());
        mainContentPanel.setBorder(new EmptyBorder(0, 0, 0, 12));

        // Panel derecho
        sidebarPanel = new JPanel();
        sidebarPanel.setLayout(new BoxLayout(sidebarPanel, BoxLayout.Y_AXIS));
        sidebarPanel.setBorder(new EmptyBorder(0, 12, 0, 0));

        splitPane.setLeftComponent(mainContentPanel);
        splitPane.setRightComponent(sidebarPanel);

        add(splitPane, BorderLayout.CENTER);
    }

    private void loadIncidencia() {
        new SwingWorker<IncidenciaData, Void>() {
            private Exception error;

            @Override
            protected IncidenciaData doInBackground() {
                try {
                    Incidencia incidencia = incidenciaController.getById(incidenciaId);
                    if (incidencia == null) {
                        return null;
                    }
                    
                    // Extraer todos los IDs necesarios ANTES de que la sesión se cierre
                    // Esto evita el error "Operation not allowed after ResultSet closed"
                    Long responsableId = null;
                    if (incidencia.getResponsable() != null) {
                        responsableId = incidencia.getResponsable().getId();
                    }
                    
                    Long estadoId = null;
                    if (incidencia.getCurrentVersion() != null && 
                        incidencia.getCurrentVersion().getEstado() != null) {
                        estadoId = incidencia.getCurrentVersion().getEstado().getId();
                    }
                    
                    return new IncidenciaData(incidencia, responsableId, estadoId);
                } catch (Exception ex) {
                    this.error = ex;
                    return null;
                }
            }

            @Override
            protected void done() {
                if (error != null) {
                    String msg = (error instanceof NotFoundException) 
                            ? "Incidencia no encontrada." 
                            : "Error al cargar incidencia: " + error.getMessage();
                    JOptionPane.showMessageDialog(IncidenciaDetailPanel.this, msg);
                    return;
                }
                try {
                    IncidenciaData data = get();
                    if (data != null && data.incidencia != null) {
                        // Actualizar descripción original antes de poblar UI
                        descripcionOriginal = data.incidencia.getDescripcion() != null 
                            ? data.incidencia.getDescripcion() 
                            : "";
                        populateUI(data.incidencia, data.responsableId, data.estadoId);
                    }
                } catch (Exception e) {
                    JOptionPane.showMessageDialog(IncidenciaDetailPanel.this, "Error inesperado.");
                }
            }
        }.execute();
    }
    
    private static class IncidenciaData {
        final Incidencia incidencia;
        final Long responsableId;
        final Long estadoId;
        
        IncidenciaData(Incidencia incidencia, Long responsableId, Long estadoId) {
            this.incidencia = incidencia;
            this.responsableId = responsableId;
            this.estadoId = estadoId;
        }
    }

    private void populateUI(Incidencia incidencia, Long responsableId, Long estadoId) {
        this.incidenciaActual = incidencia;
        
        if (comentarioTextArea != null) {
            String placeholder = "Escriba su comentario...";
            comentarioTextArea.setText(placeholder);
            comentarioTextArea.setForeground(Color.GRAY);
        }
        
        mainContentPanel.removeAll();
        sidebarPanel.removeAll();

        buildSidebar(incidencia, responsableId, estadoId);
        buildMainContent(incidencia);
        
        loadHistorial(incidencia);

        revalidate();
        repaint();
    }

    private void buildSidebar(Incidencia incidencia, Long responsableId, Long estadoId) {
        this.incidenciaActual = incidencia;
        sidebarPanel.setBorder(new TitledBorder("Información"));

        // Responsable
        JPanel responsablePanel = new JPanel(new BorderLayout());
        responsablePanel.setBorder(new EmptyBorder(5, 5, 5, 5));
        JLabel responsableLabel = new JLabel("Responsable:");
        responsableLabel.setFont(responsableLabel.getFont().deriveFont(Font.BOLD));
        responsablePanel.add(responsableLabel, BorderLayout.NORTH);
        
        comboResponsable = new JComboBox<>();
        comboResponsable.setPreferredSize(new Dimension(200, 25));
        comboResponsable.addItem(null); // Opción "Sin asignar"
        comboResponsable.setRenderer(new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(JList<?> list, Object value, int index,
                                                          boolean isSelected, boolean cellHasFocus) {
                super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
                if (value == null) {
                    setText("Sin asignar");
                } else if (value instanceof UserDetailDto) {
                    UserDetailDto usuario = (UserDetailDto) value;
                    String nombreCompleto = usuario.getNombre() != null ? usuario.getNombre() : "";
                    String apellido = usuario.getApellido() != null ? usuario.getApellido() : "";
                    if (!apellido.isEmpty()) {
                        nombreCompleto += " " + apellido;
                    }
                    setText(nombreCompleto.isEmpty() ? usuario.getEmail() : nombreCompleto);
                }
                return this;
            }
        });
        
        for (java.awt.event.ActionListener al : comboResponsable.getActionListeners()) {
            comboResponsable.removeActionListener(al);
        }
        
        comboResponsable.addActionListener(e -> {
            if (!cargandoUsuarios && comboResponsable.getSelectedItem() != null && incidenciaActual != null) {
                onResponsableChanged();
            }
        });
        
        if (responsableId != null) {
            loadUsuariosAndSelect(responsableId);
        } else {
            loadUsuariosAndSelect(null);
        }
    
        responsablePanel.add(comboResponsable, BorderLayout.CENTER);
        sidebarPanel.add(responsablePanel);

        sidebarPanel.add(Box.createVerticalStrut(5));

        // Estado
        JPanel estadoPanel = new JPanel(new BorderLayout());
        estadoPanel.setBorder(new EmptyBorder(5, 5, 5, 5));
        JLabel estadoLabel = new JLabel("Estado:");
        estadoLabel.setFont(estadoLabel.getFont().deriveFont(Font.BOLD));
        estadoPanel.add(estadoLabel, BorderLayout.NORTH);
        
        comboEstado = new JComboBox<>();
        comboEstado.setPreferredSize(new Dimension(200, 25));
        comboEstado.setRenderer(new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(JList<?> list, Object value, int index,
                                                          boolean isSelected, boolean cellHasFocus) {
                super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
                if (value instanceof IncidenciaEstado) {
                    setText(((IncidenciaEstado) value).getNombre());
                }
                return this;
            }
        });
        
        for (java.awt.event.ActionListener al : comboEstado.getActionListeners()) {
            comboEstado.removeActionListener(al);
        }
        
        comboEstado.addActionListener(e -> {
            if (!cargandoEstados && comboEstado.getSelectedItem() != null && incidenciaActual != null) {
                onEstadoChanged();
            }
        });
        
        loadEstadosAndSelect(estadoId);
    
        estadoPanel.add(comboEstado, BorderLayout.CENTER);
        sidebarPanel.add(estadoPanel);

        sidebarPanel.add(Box.createVerticalStrut(5));

        // Proyecto
        JPanel proyectoPanel = new JPanel(new BorderLayout());
        proyectoPanel.setBorder(new EmptyBorder(5, 5, 5, 5));
        JLabel proyectoLabel = new JLabel("Proyecto:");
        proyectoLabel.setFont(proyectoLabel.getFont().deriveFont(Font.BOLD));
        proyectoPanel.add(proyectoLabel, BorderLayout.NORTH);
        
        String proyectoText = incidencia.getProyecto() != null
                ? incidencia.getProyecto().getNombre()
                : "Sin proyecto";
        JLabel proyectoValue = new JLabel("<html>" + proyectoText + "</html>");
        proyectoValue.setBorder(new EmptyBorder(0, 0, 0, 0));
        proyectoPanel.add(proyectoValue, BorderLayout.CENTER);
        sidebarPanel.add(proyectoPanel);

        sidebarPanel.add(Box.createVerticalGlue());
    }

    private void loadUsuarios() {
        new SwingWorker<List<UserDetailDto>, Void>() {
            @Override
            protected List<UserDetailDto> doInBackground() {
                try {
                    return userController.getAll();
                } catch (Exception ex) {
                    return null;
                }
            }

            @Override
            protected void done() {
                try {
                    cargandoUsuarios = true; 
                    
                    List<UserDetailDto> usuarios = get();
                    if (usuarios != null) {
                        comboResponsable.removeAllItems();
                        comboResponsable.addItem(null);
                        for (UserDetailDto usuario : usuarios) {
                            comboResponsable.addItem(usuario);
                        }
                    }
                } catch (Exception e) {
                } finally {
                    cargandoUsuarios = false; 
                }
            }
        }.execute();
    }

    private void loadUsuariosAndSelect(Long responsableId) {
        new SwingWorker<List<UserDetailDto>, Void>() {
            @Override
            protected List<UserDetailDto> doInBackground() {
                try {
                    return userController.getAll();
                } catch (Exception ex) {
                    return null;
                }
            }

            @Override
            protected void done() {
                try {
                    cargandoUsuarios = true; 
                    
                    List<UserDetailDto> usuarios = get();
                    if (usuarios != null) {
                        comboResponsable.removeAllItems();
                        comboResponsable.addItem(null);
                        
                        if (usuarios.isEmpty()) {
                            cargandoUsuarios = false;
                            return;
                        }
                        
                        UserDetailDto seleccionado = null;
                        for (UserDetailDto usuario : usuarios) {
                            comboResponsable.addItem(usuario);
                            if (responsableId != null && usuario.getId() != null && usuario.getId().equals(responsableId)) {
                                seleccionado = usuario;
                            }
                        }
                        
                        if (seleccionado != null) {
                            comboResponsable.setSelectedItem(seleccionado);
                        } else {
                            comboResponsable.setSelectedItem(null); // "Sin asignar"
                        }
                    }
                } catch (Exception e) {
                } finally {
                    cargandoUsuarios = false;   
                }
            }
        }.execute();
    }

    private void loadEstadosAndSelect(Long estadoId) {
        final Long estadoIdFinal = estadoId;
        
        new SwingWorker<List<IncidenciaEstado>, Void>() {
            @Override
            protected List<IncidenciaEstado> doInBackground() {
                try {
                    return incidenciaController.getAllEstados();
                } catch (Exception ex) {
                    return null;
                }
            }

            @Override
            protected void done() {
                try {
                    cargandoEstados = true; 
                    
                    List<IncidenciaEstado> estados = get();
                    if (estados != null && !estados.isEmpty()) {
                        comboEstado.removeAllItems();
                        int indiceSeleccionado = -1;
                        int indice = 0;
                        
                        for (IncidenciaEstado estado : estados) {
                            comboEstado.addItem(estado);
                            if (estadoIdFinal != null && estado.getId() != null && estadoIdFinal.equals(estado.getId())) {
                                indiceSeleccionado = indice;
                            }
                            indice++;
                        }
                        
                        if (indiceSeleccionado >= 0) {
                            comboEstado.setSelectedIndex(indiceSeleccionado);
                        } else if (!estados.isEmpty() && estadoIdFinal == null) {
                            comboEstado.setSelectedIndex(0);
                        }
                    }
                } catch (Exception e) {
                } finally {
                    cargandoEstados = false; 
                }
            }
        }.execute();
    }

    private void onResponsableChanged() {
        if (incidenciaActual == null) return;
        
        UserDetailDto nuevoResponsable = (UserDetailDto) comboResponsable.getSelectedItem();
        Long nuevoResponsableId = nuevoResponsable != null ? nuevoResponsable.getId() : null;
        
        // Verificar si realmente cambió
        Long responsableActualId = incidenciaActual.getResponsable() != null 
                ? incidenciaActual.getResponsable().getId() 
                : null;
        
        if ((nuevoResponsableId == null && responsableActualId == null) ||
            (nuevoResponsableId != null && nuevoResponsableId.equals(responsableActualId))) {
            return; // No cambió
        }

        new SwingWorker<Void, Void>() {
            private Exception error;

            @Override
            protected Void doInBackground() {
                try {
                    Incidencia incidenciaUpdate = new Incidencia();
                    if (nuevoResponsableId != null) {
                        Usuario responsable = new Usuario();
                        responsable.setId(nuevoResponsableId);
                        incidenciaUpdate.setResponsable(responsable);
                    } else {
                        incidenciaUpdate.setResponsable(null);
                    }
                    incidenciaController.update(incidenciaId, incidenciaUpdate);
                    return null;
                } catch (Exception ex) {
                    this.error = ex;
                    return null;
                }
            }

            @Override
            protected void done() {
                if (error != null) {
                    JOptionPane.showMessageDialog(IncidenciaDetailPanel.this,
                            "Error al actualizar responsable: " + error.getMessage());
                    // Recargar para restaurar el valor anterior
                    loadIncidencia();
                    return;
                }
                // Recargar incidencia y historial
                loadIncidencia();
            }
        }.execute();
    }

    private void onEstadoChanged() {
        if (incidenciaActual == null) return;
        
        IncidenciaEstado nuevoEstado = (IncidenciaEstado) comboEstado.getSelectedItem();
        if (nuevoEstado == null || nuevoEstado.getId() == null) return;
        
        // Verificar si realmente cambió
        Long estadoActualId = incidenciaActual.getCurrentVersion() != null 
                && incidenciaActual.getCurrentVersion().getEstado() != null
                ? incidenciaActual.getCurrentVersion().getEstado().getId() 
                : null;
        
        if (nuevoEstado.getId().equals(estadoActualId)) {
            return; // No cambió
        }

        new SwingWorker<Void, Void>() {
            private Exception error;

            @Override
            protected Void doInBackground() {
                try {
                    incidenciaController.cambiarEstado(incidenciaId, nuevoEstado.getId(), currentUser);
                    return null;
                } catch (Exception ex) {
                    this.error = ex;
                    return null;
                }
            }

            @Override
            protected void done() {
                if (error != null) {
                    JOptionPane.showMessageDialog(IncidenciaDetailPanel.this,
                            "Error al cambiar estado: " + error.getMessage());
                    // Recargar para restaurar el valor anterior
                    loadIncidencia();
                    return;
                }
                // Recargar incidencia y historial
                loadIncidencia();
            }
        }.execute();
    }

    private void buildMainContent(Incidencia incidencia) {
        mainContentPanel.removeAll();
        
        JLabel titleLabel = new JLabel("Incidencia #" + incidencia.getId());
        titleLabel.setFont(titleLabel.getFont().deriveFont(Font.BOLD, 20f));
        titleLabel.setBorder(new EmptyBorder(0, 0, 10, 0));
        mainContentPanel.add(titleLabel, BorderLayout.NORTH);

        JPanel centerPanel = new JPanel();
        centerPanel.setLayout(new BoxLayout(centerPanel, BoxLayout.Y_AXIS));

        JPanel descripcionPanel = new JPanel(new BorderLayout());
        descripcionPanel.setBorder(new EmptyBorder(0, 0, 20, 0));
        JLabel descripcionLabel = new JLabel("Descripción:");
        descripcionLabel.setFont(descripcionLabel.getFont().deriveFont(Font.BOLD));
        descripcionPanel.add(descripcionLabel, BorderLayout.NORTH);
        
        // Actualizar descripción original si no está establecida
        if (descripcionOriginal == null) {
            descripcionOriginal = incidencia.getDescripcion() != null ? incidencia.getDescripcion() : "";
        }
        
        // Crear o actualizar el campo de descripción
        if (descripcionField == null) {
            descripcionField = new JTextField(descripcionOriginal);
            descripcionField.setBorder(new EmptyBorder(5, 5, 5, 5));
            descripcionField.addCaretListener(e -> {
                // Habilitar botón si hay cambios
                String textoActual = descripcionField.getText();
                boolean hayCambios = !textoActual.equals(descripcionOriginal);
                if (btnGuardarDescripcion != null) {
                    btnGuardarDescripcion.setEnabled(hayCambios);
                }
            });
        } else {
            descripcionField.setText(descripcionOriginal);
            if (btnGuardarDescripcion != null) {
                btnGuardarDescripcion.setEnabled(false);
            }
        }
        descripcionPanel.add(descripcionField, BorderLayout.CENTER);
        
        // Botón Guardar
        if (btnGuardarDescripcion == null) {
            btnGuardarDescripcion = new JButton("Guardar");
            btnGuardarDescripcion.setPreferredSize(new Dimension(100, 30));
            btnGuardarDescripcion.addActionListener(e -> guardarDescripcion());
        }
        btnGuardarDescripcion.setEnabled(false);
        
        JPanel botonPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        botonPanel.setBorder(new EmptyBorder(5, 0, 0, 0));
        botonPanel.add(btnGuardarDescripcion);
        descripcionPanel.add(botonPanel, BorderLayout.SOUTH);
        
        centerPanel.add(descripcionPanel);

        JLabel historialLabel = new JLabel("Historial:");
        historialLabel.setFont(historialLabel.getFont().deriveFont(Font.BOLD));
        historialLabel.setBorder(new EmptyBorder(0, 0, 10, 0));
        centerPanel.add(historialLabel);

        historialPanel = new JPanel();
        historialPanel.setLayout(new BoxLayout(historialPanel, BoxLayout.Y_AXIS));
        historialPanel.setBorder(new EmptyBorder(0, 0, 0, 0));
        
        JScrollPane historialScroll = new JScrollPane(historialPanel);
        historialScroll.setBorder(new EmptyBorder(0, 0, 0, 0));
        historialScroll.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
        centerPanel.add(historialScroll);

        // Panel de comentarios
        JPanel comentariosPanel = new JPanel(new BorderLayout());
        comentariosPanel.setBorder(new EmptyBorder(20, 0, 0, 0));
        
        JLabel comentariosLabel = new JLabel("Agregar comentario:");
        comentariosLabel.setFont(comentariosLabel.getFont().deriveFont(Font.BOLD));
        comentariosPanel.add(comentariosLabel, BorderLayout.NORTH);
        
        // Crear o reutilizar el campo de comentarios
        if (comentarioTextArea == null) {
            comentarioTextArea = new JTextArea(5, 0);
            comentarioTextArea.setLineWrap(true);
            comentarioTextArea.setWrapStyleWord(true);
            comentarioTextArea.setBorder(new EmptyBorder(5, 5, 5, 5));
        }
        
        String placeholder = "Escriba su comentario...";
        comentarioTextArea.setText(placeholder);
        comentarioTextArea.setForeground(Color.GRAY);
        
        for (java.awt.event.FocusListener fl : comentarioTextArea.getFocusListeners()) {
            comentarioTextArea.removeFocusListener(fl);
        }
        
        comentarioTextArea.addFocusListener(new java.awt.event.FocusAdapter() {
            @Override
            public void focusGained(java.awt.event.FocusEvent e) {
                if (comentarioTextArea.getText().equals(placeholder)) {
                    comentarioTextArea.setText("");
                    comentarioTextArea.setForeground(Color.BLACK);
                }
            }
            @Override
            public void focusLost(java.awt.event.FocusEvent e) {
                if (comentarioTextArea.getText().trim().isEmpty()) {
                    comentarioTextArea.setText(placeholder);
                    comentarioTextArea.setForeground(Color.GRAY);
                }
            }
        });
        
        JScrollPane comentarioScroll = new JScrollPane(comentarioTextArea);
        comentarioScroll.setBorder(new EmptyBorder(5, 0, 5, 0));
        comentariosPanel.add(comentarioScroll, BorderLayout.CENTER);
        
        if (btnEnviarComentario == null) {
            btnEnviarComentario = new JButton("Enviar");
            btnEnviarComentario.setPreferredSize(new Dimension(100, 30));
            btnEnviarComentario.addActionListener(e -> enviarComentario());
        }
        
        JPanel botonComentarioPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        botonComentarioPanel.add(btnEnviarComentario);
        comentariosPanel.add(botonComentarioPanel, BorderLayout.SOUTH);
        
        centerPanel.add(comentariosPanel);

        mainContentPanel.add(centerPanel, BorderLayout.CENTER);

    }

    private void loadHistorial(Incidencia incidencia) {
        if (historialPanel == null) {
            return;
        }
        
        new SwingWorker<List<HistorialItem>, Void>() {
            private Exception error;

            @Override
            protected List<HistorialItem> doInBackground() {
                try {
                    List<IncidenciaVersion> versiones = incidenciaController.getHistorialVersiones(incidenciaId);
                    List<Comentario> comentarios = comentarioController.findByIncidencia(incidenciaId);
                    List<HistorialItem> items = new ArrayList<>();
                    
                    if (versiones != null) {
                        for (IncidenciaVersion version : versiones) {
                            items.add(new HistorialItem(version.getCreatedAt(), version, null));
                        }
                    }
                    
                    if (comentarios != null) {
                        for (Comentario comentario : comentarios) {
                            items.add(new HistorialItem(comentario.getCreatedAt(), null, comentario));
                        }
                    }

                    items.sort(Comparator.comparing(HistorialItem::getFecha).reversed());

                    return items;
                } catch (Exception ex) {
                    this.error = ex;
                    return null;
                }
            }

            @Override
            protected void done() {
                if (error != null) {
                    JOptionPane.showMessageDialog(IncidenciaDetailPanel.this,
                            "Error al cargar historial: " + error.getMessage());
                    return;
                }
                try {
                    List<HistorialItem> items = get();
                    if (items != null && historialPanel != null) {
                        populateHistorial(items);
                    }
                } catch (Exception e) {
                    JOptionPane.showMessageDialog(IncidenciaDetailPanel.this, "Error inesperado.");
                }
            }
        }.execute();
    }

    private void populateHistorial(List<HistorialItem> items) {
        if (historialPanel == null) {
            return; 
        }
        historialPanel.removeAll();
        
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

        for (HistorialItem item : items) {
            JPanel itemPanel = new JPanel();
            itemPanel.setLayout(new BorderLayout());
            itemPanel.setBorder(new EmptyBorder(5, 0, 10, 0));
            itemPanel.setAlignmentX(Component.LEFT_ALIGNMENT);

            if (item.version != null) {
                // Es un cambio de estado
                JLabel tipoLabel = new JLabel("Cambio de estado");
                tipoLabel.setFont(tipoLabel.getFont().deriveFont(Font.BOLD, 12f));
                tipoLabel.setForeground(new Color(0, 100, 200));
                itemPanel.add(tipoLabel, BorderLayout.NORTH);

                String estadoNombre = item.version.getEstado() != null 
                        ? item.version.getEstado().getNombre() 
                        : "Desconocido";
                String usuarioNombre = "Usuario desconocido";
                if (item.version.getCreatedBy() != null) {
                    String nombre = item.version.getCreatedBy().getNombre() != null ? item.version.getCreatedBy().getNombre() : "";
                    String apellido = item.version.getCreatedBy().getApellido() != null ? item.version.getCreatedBy().getApellido() : "";
                    usuarioNombre = nombre;
                    if (!apellido.isEmpty()) {
                        usuarioNombre += " " + apellido;
                    }
                    if (usuarioNombre.trim().isEmpty()) {
                        usuarioNombre = item.version.getCreatedBy().getEmail() != null ? item.version.getCreatedBy().getEmail() : "Usuario desconocido";
                    }
                }
                
                JLabel contenidoLabel = new JLabel("<html>Estado: <b>" + estadoNombre + "</b><br>" +
                        "Por: " + usuarioNombre + "<br>" +
                        "Fecha: " + item.fecha.format(formatter) + "</html>");
                contenidoLabel.setBorder(new EmptyBorder(3, 15, 0, 0));
                itemPanel.add(contenidoLabel, BorderLayout.CENTER);
            } else if (item.comentario != null) {
                // Es un comentario
                JLabel tipoLabel = new JLabel("Comentario");
                tipoLabel.setFont(tipoLabel.getFont().deriveFont(Font.BOLD, 12f));
                tipoLabel.setForeground(new Color(100, 150, 0));
                itemPanel.add(tipoLabel, BorderLayout.NORTH);

                String usuarioNombre = "Usuario desconocido";
                if (item.comentario.getCreatedBy() != null) {
                    String nombre = item.comentario.getCreatedBy().getNombre() != null ? item.comentario.getCreatedBy().getNombre() : "";
                    String apellido = item.comentario.getCreatedBy().getApellido() != null ? item.comentario.getCreatedBy().getApellido() : "";
                    usuarioNombre = nombre;
                    if (!apellido.isEmpty()) {
                        usuarioNombre += " " + apellido;
                    }
                    if (usuarioNombre.trim().isEmpty()) {
                        usuarioNombre = item.comentario.getCreatedBy().getEmail() != null ? item.comentario.getCreatedBy().getEmail() : "Usuario desconocido";
                    }
                }
                String texto = item.comentario.getTexto() != null ? item.comentario.getTexto() : "";
                
                JLabel contenidoLabel = new JLabel("<html>" + texto + "<br>" +
                        "<i>Por: " + usuarioNombre + " - " + item.fecha.format(formatter) + "</i></html>");
                contenidoLabel.setBorder(new EmptyBorder(3, 15, 0, 0));
                itemPanel.add(contenidoLabel, BorderLayout.CENTER);
            }

            historialPanel.add(itemPanel);
        }

        historialPanel.add(Box.createVerticalGlue());
        historialPanel.revalidate();
        historialPanel.repaint();
    }

    private static class HistorialItem {
        LocalDateTime fecha;
        IncidenciaVersion version;
        Comentario comentario;

        HistorialItem(LocalDateTime fecha, IncidenciaVersion version, Comentario comentario) {
            this.fecha = fecha;
            this.version = version;
            this.comentario = comentario;
        }

        LocalDateTime getFecha() {
            return fecha;
        }
    }

    private void guardarDescripcion() {
        String nuevaDescripcion = descripcionField.getText();
        
        new SwingWorker<Void, Void>() {
            private Exception error;

            @Override
            protected Void doInBackground() {
                try {
                    Incidencia incidenciaUpdate = new Incidencia();
                    incidenciaUpdate.setDescripcion(nuevaDescripcion);
                    incidenciaController.update(incidenciaId, incidenciaUpdate);
                    return null;
                } catch (Exception ex) {
                    this.error = ex;
                    return null;
                }
            }

            @Override
            protected void done() {
                if (error != null) {
                    JOptionPane.showMessageDialog(IncidenciaDetailPanel.this,
                            "Error al actualizar descripción: " + error.getMessage());
                    // Restaurar valor original
                    descripcionField.setText(descripcionOriginal);
                    btnGuardarDescripcion.setEnabled(false);
                    return;
                }
                // Actualizar descripción original y deshabilitar botón
                descripcionOriginal = nuevaDescripcion;
                btnGuardarDescripcion.setEnabled(false);
            }
        }.execute();
    }

    private void enviarComentario() {
        String textoComentario = comentarioTextArea.getText().trim();
        String placeholder = "Escriba su comentario...";
        
        // Validar que no esté vacío o sea solo el placeholder
        if (textoComentario.isEmpty() || textoComentario.equals(placeholder)) {
            JOptionPane.showMessageDialog(IncidenciaDetailPanel.this,
                    "Por favor ingrese un comentario antes de enviar.");
            return;
        }

        new SwingWorker<Void, Void>() {
            private Exception error;

            @Override
            protected Void doInBackground() {
                try {
                    Comentario comentario = new Comentario();
                    Incidencia incidenciaRef = new Incidencia() {
                        @Override
                        public Long getId() {
                            return incidenciaId;
                        }
                    };
                    comentario.setIncidencia(incidenciaRef);
                    comentario.setTexto(textoComentario);
                    
                    comentarioController.create(comentario, currentUser);
                    return null;
                } catch (Exception ex) {
                    this.error = ex;
                    return null;
                }
            }

            @Override
            protected void done() {
                if (error != null) {
                    JOptionPane.showMessageDialog(IncidenciaDetailPanel.this,
                            "Error al enviar comentario: " + error.getMessage());
                    return;
                }
                // Limpiar campo y mostrar placeholder
                comentarioTextArea.setText(placeholder);
                comentarioTextArea.setForeground(Color.GRAY);
                // Remover focus para que el placeholder se muestre correctamente
                comentarioTextArea.setFocusable(false);
                comentarioTextArea.setFocusable(true);
                // Recargar historial para mostrar el nuevo comentario
                loadHistorial(incidenciaActual);
            }
        }.execute();
    }
}

