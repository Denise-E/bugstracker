package ar.edu.up.bugtracker.ui.incidencias;

import ar.edu.up.bugtracker.controller.ComentarioController;
import ar.edu.up.bugtracker.controller.IncidenciaController;
import ar.edu.up.bugtracker.exceptions.NotFoundException;
import ar.edu.up.bugtracker.models.Comentario;
import ar.edu.up.bugtracker.models.Incidencia;
import ar.edu.up.bugtracker.models.IncidenciaVersion;
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
    private final UserLoggedInDto currentUser;
    private final Long incidenciaId;

    private JPanel mainContentPanel;
    private JPanel sidebarPanel;
    private JPanel historialPanel;

    public IncidenciaDetailPanel(IncidenciaController incidenciaController,
                                ComentarioController comentarioController,
                                UserLoggedInDto currentUser,
                                Long incidenciaId) {
        this.incidenciaController = incidenciaController;
        this.comentarioController = comentarioController;
        this.currentUser = currentUser;
        this.incidenciaId = incidenciaId;
        buildUI();
        loadIncidencia();
    }

    private void buildUI() {
        setLayout(new BorderLayout());
        setBorder(new EmptyBorder(12, 12, 12, 12));

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
        new SwingWorker<Incidencia, Void>() {
            private Exception error;

            @Override
            protected Incidencia doInBackground() {
                try {
                    return incidenciaController.getById(incidenciaId);
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
                    Incidencia incidencia = get();
                    if (incidencia != null) {
                        populateUI(incidencia);
                    }
                } catch (Exception e) {
                    JOptionPane.showMessageDialog(IncidenciaDetailPanel.this, "Error inesperado.");
                }
            }
        }.execute();
    }

    private void populateUI(Incidencia incidencia) {
        mainContentPanel.removeAll();
        sidebarPanel.removeAll();

        buildSidebar(incidencia);
        buildMainContent(incidencia);

        revalidate();
        repaint();
    }

    private void buildSidebar(Incidencia incidencia) {
        sidebarPanel.setBorder(new TitledBorder("Información"));

        // Persona asignada
        JPanel responsablePanel = new JPanel(new BorderLayout());
        responsablePanel.setBorder(new EmptyBorder(5, 5, 5, 5));
        JLabel responsableLabel = new JLabel("Responsable:");
        responsableLabel.setFont(responsableLabel.getFont().deriveFont(Font.BOLD));
        responsablePanel.add(responsableLabel, BorderLayout.NORTH);
        
        String responsableText = incidencia.getResponsable() != null
                ? incidencia.getResponsable().getNombre() + " " + 
                  (incidencia.getResponsable().getApellido() != null ? incidencia.getResponsable().getApellido() : "")
                : "Sin asignar";
        JLabel responsableValue = new JLabel("<html>" + responsableText + "</html>");
        responsableValue.setBorder(new EmptyBorder(3, 0, 0, 0));
        responsablePanel.add(responsableValue, BorderLayout.CENTER);
        sidebarPanel.add(responsablePanel);

        sidebarPanel.add(Box.createVerticalStrut(10));

        // Estado
        JPanel estadoPanel = new JPanel(new BorderLayout());
        estadoPanel.setBorder(new EmptyBorder(5, 5, 5, 5));
        JLabel estadoLabel = new JLabel("Estado:");
        estadoLabel.setFont(estadoLabel.getFont().deriveFont(Font.BOLD));
        estadoPanel.add(estadoLabel, BorderLayout.NORTH);
        
        String estadoText = incidencia.getCurrentVersion() != null && incidencia.getCurrentVersion().getEstado() != null
                ? incidencia.getCurrentVersion().getEstado().getNombre()
                : "NUEVA";
        JLabel estadoValue = new JLabel(estadoText);
        estadoValue.setBorder(new EmptyBorder(3, 0, 0, 0));
        estadoPanel.add(estadoValue, BorderLayout.CENTER);
        sidebarPanel.add(estadoPanel);

        sidebarPanel.add(Box.createVerticalStrut(10));

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
        proyectoValue.setBorder(new EmptyBorder(3, 0, 0, 0));
        proyectoPanel.add(proyectoValue, BorderLayout.CENTER);
        sidebarPanel.add(proyectoPanel);

        sidebarPanel.add(Box.createVerticalGlue());
    }

    private void buildMainContent(Incidencia incidencia) {
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
        
        JTextArea descripcionText = new JTextArea(incidencia.getDescripcion() != null ? incidencia.getDescripcion() : "");
        descripcionText.setEditable(false);
        descripcionText.setLineWrap(true);
        descripcionText.setWrapStyleWord(true);
        descripcionText.setBackground(getBackground());
        descripcionText.setBorder(new EmptyBorder(5, 0, 0, 0));
        descripcionPanel.add(descripcionText, BorderLayout.CENTER);
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

        mainContentPanel.add(centerPanel, BorderLayout.CENTER);

        loadHistorial(incidencia);
    }

    private void loadHistorial(Incidencia incidencia) {
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

                    // Ordenar por fecha
                    items.sort(Comparator.comparing(HistorialItem::getFecha));

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
                    if (items != null) {
                        populateHistorial(items);
                    }
                } catch (Exception e) {
                    JOptionPane.showMessageDialog(IncidenciaDetailPanel.this, "Error inesperado.");
                }
            }
        }.execute();
    }

    private void populateHistorial(List<HistorialItem> items) {
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
                String usuarioNombre = item.version.getCreatedBy() != null
                        ? item.version.getCreatedBy().getNombre()
                        : "Usuario desconocido";
                
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

                String usuarioNombre = item.comentario.getCreatedBy() != null
                        ? item.comentario.getCreatedBy().getNombre()
                        : "Usuario desconocido";
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
}

