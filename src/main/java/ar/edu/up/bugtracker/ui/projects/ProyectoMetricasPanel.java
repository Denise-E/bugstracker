package ar.edu.up.bugtracker.ui.projects;

import ar.edu.up.bugtracker.controller.IncidenciaController;
import ar.edu.up.bugtracker.controller.ProyectoController;
import ar.edu.up.bugtracker.exceptions.NotFoundException;
import ar.edu.up.bugtracker.models.Incidencia;
import ar.edu.up.bugtracker.models.IncidenciaEstado;
import ar.edu.up.bugtracker.models.Proyecto;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.border.LineBorder;
import java.awt.*;
import java.util.List;
import java.util.function.Consumer;

public class ProyectoMetricasPanel extends JPanel {

    private final ProyectoController proyectoController;
    private final IncidenciaController incidenciaController;
    private final Long proyectoId;
    private final Consumer<Long> onVolver;

    private JLabel lblTotalIncidencias;
    private JLabel lblPorcentajeCompletado;
    private JLabel lblTareasPendientes;

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

        JPanel centerPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 20, 20));
        centerPanel.setBorder(new EmptyBorder(20, 20, 20, 20));

        JPanel metricBox1 = createMetricBox("Total de incidencias", "0", lblTotalIncidencias = new JLabel("0"));
        centerPanel.add(metricBox1);

        JPanel metricBox2 = createMetricBox("Porcentaje completado", "0%", lblPorcentajeCompletado = new JLabel("0%"));
        centerPanel.add(metricBox2);

        JPanel metricBox3 = createMetricBox("Total de tareas pendientes", "0", lblTareasPendientes = new JLabel("0"));
        centerPanel.add(metricBox3);

        add(centerPanel, BorderLayout.CENTER);
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
        new SwingWorker<MetricasData, Void>() {
            private Exception error;

            @Override
            protected MetricasData doInBackground() {
                try {
                    List<Incidencia> incidencias = incidenciaController.findByProyecto(proyectoId);
                    
                    int totalIncidencias = incidencias != null ? incidencias.size() : 0;
                    int completadas = 0;
                    int pendientes = 0;

                    if (incidencias != null) {
                        for (Incidencia incidencia : incidencias) {
                            if (incidencia.getCurrentVersion() != null && 
                                incidencia.getCurrentVersion().getEstado() != null) {
                                Long estadoId = incidencia.getCurrentVersion().getEstado().getId();
                                
                                // Estados terminada, cancelada o duplicada
                                if (estadoId != null && (estadoId == 5 || estadoId == 9 || estadoId == 10)) {
                                    completadas++;
                                }
                                // Estados pendiente, en proceso, bloqueada o en revision
                                if (estadoId != null && (estadoId == 1 || estadoId == 2 || estadoId == 3 || estadoId == 4)) {
                                    pendientes++;
                                }
                            }
                        }
                    }

                    double porcentajeCompletado = totalIncidencias > 0 
                        ? (completadas * 100.0 / totalIncidencias) 
                        : 0.0;

                    return new MetricasData(totalIncidencias, porcentajeCompletado, pendientes);
                } catch (Exception ex) {
                    this.error = ex;
                    return null;
                }
            }

            @Override
            protected void done() {
                if (error != null) {
                    String msg = (error instanceof NotFoundException)
                            ? "Proyecto no encontrado."
                            : "Error al cargar métricas: " + error.getMessage();
                    JOptionPane.showMessageDialog(ProyectoMetricasPanel.this, msg);
                    return;
                }
                try {
                    MetricasData data = get();
                    if (data != null) {
                        lblTotalIncidencias.setText(String.valueOf(data.totalIncidencias));
                        lblPorcentajeCompletado.setText(String.format("%.1f%%", data.porcentajeCompletado));
                        lblTareasPendientes.setText(String.valueOf(data.tareasPendientes));
                    }
                } catch (Exception e) {
                    JOptionPane.showMessageDialog(ProyectoMetricasPanel.this, "Error inesperado.");
                }
            }
        }.execute();
    }

    private static class MetricasData {
        final int totalIncidencias;
        final double porcentajeCompletado;
        final int tareasPendientes;

        MetricasData(int totalIncidencias, double porcentajeCompletado, int tareasPendientes) {
            this.totalIncidencias = totalIncidencias;
            this.porcentajeCompletado = porcentajeCompletado;
            this.tareasPendientes = tareasPendientes;
        }
    }
}
