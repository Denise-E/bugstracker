package ar.edu.up.bugtracker.ui.projects;

import ar.edu.up.bugtracker.controller.IncidenciaController;
import ar.edu.up.bugtracker.controller.ProyectoController;
import ar.edu.up.bugtracker.models.Incidencia;
import ar.edu.up.bugtracker.models.IncidenciaEstado;
import ar.edu.up.bugtracker.models.Proyecto;
import ar.edu.up.bugtracker.ui.components.SwingWorkerFactory;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.border.LineBorder;
import java.awt.*;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

public class ProyectoMetricasPanel extends JPanel {

    private final ProyectoController proyectoController;
    private final IncidenciaController incidenciaController;
    private final Long proyectoId;
    private final Consumer<Long> onVolver;

    private JLabel lblTotalIncidencias;
    private JLabel lblPorcentajeCompletado;
    private JLabel lblTareasPendientes;
    private JPanel estadosListPanel;

    public ProyectoMetricasPanel(ProyectoController proyectoController,
                                 IncidenciaController incidenciaController,
                                 Long proyectoId,
                                 Consumer<Long> onVolver) {
        this.proyectoController = proyectoController;
        this.incidenciaController = incidenciaController;
        this.proyectoId = proyectoId;
        this.onVolver = onVolver;
        buildUI();
        loadMetricas();
    }

    private void buildUI() {
        setLayout(new BorderLayout());
        setBorder(new EmptyBorder(12, 12, 12, 12));

        JPanel topPanel = new JPanel(new BorderLayout());
        JButton btnVolver = new JButton("← Volver");
        btnVolver.addActionListener(e -> {
            if (onVolver != null) {
                onVolver.accept(proyectoId);
            }
        });
        topPanel.add(btnVolver, BorderLayout.WEST);
        add(topPanel, BorderLayout.NORTH);

        JPanel mainPanel = new JPanel();
        mainPanel.setLayout(new BoxLayout(mainPanel, BoxLayout.Y_AXIS));
        
        JPanel metricsBoxesPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 20, 20));
        metricsBoxesPanel.setBorder(new EmptyBorder(20, 20, 20, 20));

        JPanel metricBox1 = createMetricBox("Total de incidencias", "0", lblTotalIncidencias = new JLabel("0"));
        metricsBoxesPanel.add(metricBox1);

        JPanel metricBox2 = createMetricBox("Porcentaje completado", "0%", lblPorcentajeCompletado = new JLabel("0%"));
        metricsBoxesPanel.add(metricBox2);

        JPanel metricBox3 = createMetricBox("<html>Total de tareas<br>pendientes</html>", "0", lblTareasPendientes = new JLabel("0"));
        metricsBoxesPanel.add(metricBox3);

        mainPanel.add(metricsBoxesPanel);
        
        JPanel estadosSectionPanel = new JPanel(new BorderLayout());
        estadosSectionPanel.setBorder(new EmptyBorder(20, 20, 20, 20));
        
        JLabel estadosTitleLabel = new JLabel("Total de incidencias por estado");
        estadosTitleLabel.setFont(estadosTitleLabel.getFont().deriveFont(Font.BOLD, 16f));
        estadosSectionPanel.add(estadosTitleLabel, BorderLayout.NORTH);
        
        estadosListPanel = new JPanel();
        estadosListPanel.setLayout(new BoxLayout(estadosListPanel, BoxLayout.Y_AXIS));
        estadosListPanel.setBorder(new EmptyBorder(10, 0, 0, 0));
        
        JScrollPane estadosScroll = new JScrollPane(estadosListPanel);
        estadosScroll.setBorder(new EmptyBorder(0, 0, 0, 0));
        estadosScroll.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
        estadosScroll.setPreferredSize(new Dimension(400, 200));
        estadosSectionPanel.add(estadosScroll, BorderLayout.CENTER);
        
        mainPanel.add(estadosSectionPanel);
        
        add(mainPanel, BorderLayout.CENTER);
    }

    private JPanel createMetricBox(String titulo, String valorInicial, JLabel valorLabel) {
        JPanel box = new JPanel();
        box.setLayout(new BorderLayout());
        box.setBorder(new LineBorder(Color.GRAY, 1));
        box.setBackground(Color.WHITE);
        box.setPreferredSize(new Dimension(180, 180));

        JLabel tituloLabel = new JLabel(titulo);
        tituloLabel.setFont(tituloLabel.getFont().deriveFont(Font.BOLD, 14f));
        tituloLabel.setHorizontalAlignment(SwingConstants.CENTER);
        tituloLabel.setBorder(new EmptyBorder(15, 10, 10, 10));
        box.add(tituloLabel, BorderLayout.NORTH);

        valorLabel.setText(valorInicial);
        valorLabel.setFont(valorLabel.getFont().deriveFont(Font.BOLD, 36f));
        valorLabel.setHorizontalAlignment(SwingConstants.CENTER);
        valorLabel.setForeground(new Color(0, 100, 200));
        JPanel valorPanel = new JPanel(new BorderLayout());
        valorPanel.setBorder(new EmptyBorder(10, 10, 15, 10));
        valorPanel.add(valorLabel, BorderLayout.CENTER);
        box.add(valorPanel, BorderLayout.CENTER);

        return box;
    }

    private void loadMetricas() {
        SwingWorkerFactory.createWithAutoErrorHandling(
            this,
            () -> {
                List<Incidencia> incidencias = incidenciaController.findByProyecto(proyectoId);
                List<IncidenciaEstado> estados = incidenciaController.getAllEstados();
                
                int totalIncidencias = incidencias != null ? incidencias.size() : 0;
                int completadas = 0;
                int pendientes = 0;
                
                Map<Long, Integer> conteoPorEstado = new HashMap<>();
                
                if (estados != null) {
                    for (IncidenciaEstado estado : estados) {
                        if (estado.getId() != null) {
                            conteoPorEstado.put(estado.getId(), 0);
                        }
                    }
                }

                if (incidencias != null) {
                    for (Incidencia incidencia : incidencias) {
                        if (incidencia.getCurrentVersion() != null && 
                            incidencia.getCurrentVersion().getEstado() != null) {
                            Long estadoId = incidencia.getCurrentVersion().getEstado().getId();
                            
                            if (estadoId != null) {
                                conteoPorEstado.put(estadoId, conteoPorEstado.getOrDefault(estadoId, 0) + 1);
                                
                                // Estados terminada, cancelada o duplicada
                                if (estadoId == 5 || estadoId == 9 || estadoId == 10) {
                                    completadas++;
                                }
                                // Estados pendiente, en proceso, bloqueada o en revision
                                if (estadoId == 1 || estadoId == 2 || estadoId == 3 || estadoId == 4) {
                                    pendientes++;
                                }
                            }
                        }
                    }
                }

                double porcentajeCompletado = totalIncidencias > 0 
                    ? (completadas * 100.0 / totalIncidencias) 
                    : 0.0;

                return new MetricasData(totalIncidencias, porcentajeCompletado, pendientes, estados, conteoPorEstado);
            },
            data -> {
                if (data != null) {
                    lblTotalIncidencias.setText(String.valueOf(data.totalIncidencias));
                    lblPorcentajeCompletado.setText(String.format("%.1f%%", data.porcentajeCompletado));
                    lblTareasPendientes.setText(String.valueOf(data.tareasPendientes));
                    
                    populateEstadosList(data.estados, data.conteoPorEstado);
                }
            }
        ).execute();
    }
    
    private void populateEstadosList(List<IncidenciaEstado> estados, Map<Long, Integer> conteoPorEstado) {
        if (estadosListPanel == null) {
            return;
        }
        
        estadosListPanel.removeAll();
        
        if (estados != null && !estados.isEmpty()) {
            for (IncidenciaEstado estado : estados) {
                int cantidad = conteoPorEstado.getOrDefault(estado.getId(), 0);
                
                JLabel estadoLabel = new JLabel(estado.getNombre() + ": " + cantidad);
                estadoLabel.setFont(estadoLabel.getFont().deriveFont(14f));
                estadoLabel.setBorder(new EmptyBorder(5, 10, 5, 10));
                estadoLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
                
                estadosListPanel.add(estadoLabel);
            }
        }
        
        estadosListPanel.revalidate();
        estadosListPanel.repaint();
    }

    private static class MetricasData {
        final int totalIncidencias;
        final double porcentajeCompletado;
        final int tareasPendientes;
        final List<IncidenciaEstado> estados;
        final Map<Long, Integer> conteoPorEstado;

        MetricasData(int totalIncidencias, double porcentajeCompletado, int tareasPendientes, 
                     List<IncidenciaEstado> estados, Map<Long, Integer> conteoPorEstado) {
            this.totalIncidencias = totalIncidencias;
            this.porcentajeCompletado = porcentajeCompletado;
            this.tareasPendientes = tareasPendientes;
            this.estados = estados;
            this.conteoPorEstado = conteoPorEstado;
        }
    }
}
