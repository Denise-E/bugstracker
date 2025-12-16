# Análisis Técnico del Proyecto BugTracker

## Backend: Manejo de Excepciones

### ✅ Estado Actual

#### Jerarquía de Excepciones
El proyecto tiene una jerarquía bien definida de excepciones personalizadas:

```
AppException (RuntimeException)
├── BusinessException
│   ├── ValidationException
│   ├── NotFoundException
│   ├── AuthException
│   └── ForbiddenException
└── DaoException
```

**Ubicación**: `src/main/java/ar/edu/up/bugtracker/exceptions/`

#### Cómo Funciona Actualmente

1. **En los DAOs**: 
   - Los DAOs capturan excepciones de bajo nivel (SQL, JPA) y las envuelven en `DaoException`
   - Ejemplo: `ComentarioDao.findByIncidencia()` captura excepciones y lanza `DaoException`

2. **En los Services**:
   - Los servicios capturan `DaoException` y otras excepciones de runtime
   - Las convierten en excepciones de negocio (`AppException`, `ValidationException`, `NotFoundException`, etc.)
   - Ejemplo en `IncidenciaService.update()`:
     ```java
     try {
         // ... lógica ...
     } catch (NotFoundException ex) {
         rollbackSilently();
         throw ex; // Re-lanza NotFoundException
     } catch (RuntimeException ex) {
         rollbackSilently();
         throw new AppException("Error actualizando incidencia", ex);
     }
     ```

3. **En los Controllers**:
   - Los controladores validan parámetros y lanzan `ValidationException`
   - Delegan al servicio, pero **NO capturan ni manejan excepciones**
   - Las excepciones se propagan directamente al frontend
   - Ejemplo en `IncidenciaController.update()`:
     ```java
     public void update(Long id, Incidencia incidencia) {
         if (id == null) {
             throw new ValidationException("ID requerido");
         }
         service.update(id, incidencia); // Excepciones se propagan sin captura
     }
     ```

4. **En el Frontend (UI)**:
   - Cada `SwingWorker` captura excepciones en `doInBackground()`
   - En `done()`, verifica el tipo de excepción y muestra mensajes con `JOptionPane`
   - Ejemplo en `ProyectosListPanel.onDeleteRow()`:
     ```java
     @Override
     protected void done() {
         if (error != null) {
             String msg;
             if (error instanceof ForbiddenException) {
                 msg = "No tenés permisos para eliminar proyectos.";
             } else if (error instanceof NotFoundException) {
                 msg = "Proyecto no encontrado.";
             } else {
                 msg = "Error al eliminar: " + error.getMessage();
             }
             JOptionPane.showMessageDialog(ProyectosListPanel.this, msg);
         }
     }
     ```

### ⚠️ Problemas Identificados

1. **Falta de Capa de Manejo Centralizado**:
   - Las excepciones se propagan directamente desde servicios/controladores al frontend
   - No hay un punto centralizado donde se manejen las excepciones
   - Cada panel de UI repite la misma lógica de manejo de errores

2. **Duplicación de Código**:
   - El manejo de excepciones se repite en cada `SwingWorker.done()`
   - Cada panel tiene su propia lógica para determinar qué mensaje mostrar según el tipo de excepción
   - Ejemplo: `ProyectoDialog`, `IncidenciaDialog`, `ProyectosListPanel` tienen código similar

3. **Inconsistencias en Mensajes**:
   - Algunos paneles manejan `ForbiddenException`, otros no
   - Los mensajes de error varían entre paneles
   - No hay un estándar para mensajes de error

### 💡 Recomendaciones

1. **Crear un ExceptionHandler Centralizado**:
   ```java
   public class ExceptionHandler {
       public static String getErrorMessage(Exception ex) {
           if (ex instanceof ValidationException) {
               return ex.getMessage();
           } else if (ex instanceof NotFoundException) {
               return "Recurso no encontrado.";
           } else if (ex instanceof ForbiddenException) {
               return "No tenés permisos para realizar esta acción.";
           } else if (ex instanceof AuthException) {
               return "Debes estar autenticado para realizar esta acción.";
           } else {
               return "Ocurrió un error inesperado: " + ex.getMessage();
           }
       }
   }
   ```

2. **Crear un SwingWorker Base**:
   ```java
   public abstract class BaseSwingWorker<T, V> extends SwingWorker<T, V> {
       protected Exception error;
       
       @Override
       protected void done() {
           if (error != null) {
               String msg = ExceptionHandler.getErrorMessage(error);
               JOptionPane.showMessageDialog(null, msg);
           } else {
               onSuccess();
           }
       }
       
       protected abstract void onSuccess();
   }
   ```

---

## Frontend: Reutilización de Botones

### ❌ Estado Actual: NO HAY REUTILIZACIÓN

Los botones "Ver", "Editar" y "Eliminar" **NO están definidos una única vez**. Se crean múltiples veces en diferentes clases:

#### Ubicaciones donde se definen:

1. **`ProyectosListPanel.java`** (líneas 184-186, 211-213):
   ```java
   private class ActionsRenderer extends JPanel implements TableCellRenderer {
       private final JButton btnEdit = new JButton("Editar");
       private final JButton btnView = new JButton("Ver");
       private final JButton btnDelete = new JButton("Eliminar");
   }
   
   private class ActionsEditor extends AbstractCellEditor implements TableCellEditor {
       private final JButton btnEdit = new JButton("Editar");
       private final JButton btnView = new JButton("Ver");
       private final JButton btnDelete = new JButton("Eliminar");
   }
   ```

2. **`ProyectoDetailPanel.java`** (líneas 326-327, 355-356):
   ```java
   private class ActionsRenderer extends JPanel implements TableCellRenderer {
       private final JButton btnView = new JButton("Ver");
       private final JButton btnDelete = new JButton("Eliminar");
   }
   
   private class ActionsEditor extends AbstractCellEditor implements TableCellEditor {
       private final JButton btnView = new JButton("Ver");
       private final JButton btnDelete = new JButton("Eliminar");
   }
   ```

3. **`UsuariosListPanel.java`** (líneas 132-133, 153-154):
   ```java
   private class ActionsRenderer extends JPanel implements TableCellRenderer {
       private final JButton btnEdit = new JButton("Editar");
       private final JButton btnDelete = new JButton("Eliminar");
   }
   
   private class ActionsEditor extends AbstractCellEditor implements TableCellEditor {
       private final JButton btnEdit = new JButton("Editar");
       private final JButton btnDelete = new JButton("Eliminar");
   }
   ```

### Cómo Funciona Actualmente

Cada panel tiene su propia implementación de `ActionsRenderer` y `ActionsEditor`:

1. **ActionsRenderer**: Se usa para renderizar los botones en la tabla cuando NO están siendo editados
2. **ActionsEditor**: Se usa cuando el usuario hace clic en la celda (modo edición)

**Flujo de acción**:
- Usuario hace clic en una celda de "Acciones"
- Se activa `ActionsEditor.getTableCellEditorComponent()`
- Se muestran los botones con sus `ActionListener` configurados
- Al hacer clic en un botón:
  - Se llama a `stopCellEditing()` para cerrar el editor
  - Se ejecuta el método correspondiente (`onEditRow()`, `onViewRow()`, `onDeleteRow()`)
  - Cada método obtiene la entidad de la fila y ejecuta la acción correspondiente

**Ejemplo en `ProyectosListPanel`**:
```java
btnEdit.addActionListener(e -> {
    int row = editingRow;
    stopCellEditing();
    onEditRow(row); // Obtiene Proyecto y abre ProyectoDialog
});

private void onEditRow(int row) {
    Proyecto proyecto = tableModel.getAt(row);
    ProyectoDialog dlg = new ProyectoDialog(...);
    dlg.setVisible(true);
}
```

### ⚠️ Problemas

1. **Duplicación de Código**: Cada panel repite la misma lógica de creación y configuración de botones
2. **Inconsistencias Visuales**: Los botones pueden tener diferentes estilos entre paneles
3. **Mantenimiento Difícil**: Cambios en el comportamiento requieren modificar múltiples archivos

### 💡 Recomendación: Crear Componentes Reutilizables

Crear componentes genéricos en `src/main/java/ar/edu/up/bugtracker/ui/components/`:

```java
// TableActionsRenderer.java
public class TableActionsRenderer extends JPanel implements TableCellRenderer {
    private final List<JButton> buttons;
    
    public TableActionsRenderer(List<String> buttonLabels) {
        setLayout(new FlowLayout(FlowLayout.RIGHT, 6, 2));
        buttons = new ArrayList<>();
        for (String label : buttonLabels) {
            JButton btn = new JButton(label);
            buttons.add(btn);
            add(btn);
        }
        setOpaque(true);
    }
    
    @Override
    public Component getTableCellRendererComponent(JTable table, Object value, 
                                                   boolean isSelected, boolean hasFocus, 
                                                   int row, int column) {
        setBackground(isSelected ? table.getSelectionBackground() : table.getBackground());
        return this;
    }
}

// TableActionsEditor.java
public class TableActionsEditor extends AbstractCellEditor implements TableCellEditor {
    private final JPanel panel;
    private final List<ActionListener> listeners;
    private int editingRow = -1;
    
    public TableActionsEditor(List<String> buttonLabels, List<ActionListener> listeners) {
        this.listeners = listeners;
        panel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 6, 2));
        for (int i = 0; i < buttonLabels.size(); i++) {
            JButton btn = new JButton(buttonLabels.get(i));
            btn.addActionListener(e -> {
                editingRow = getCurrentRow();
                stopCellEditing();
                if (i < listeners.size()) {
                    listeners.get(i).actionPerformed(e);
                }
            });
            panel.add(btn);
        }
    }
    
    // ... implementación de TableCellEditor ...
}
```

---

## Frontend: Arquitectura de Componentes

### ⚠️ Estado Actual: Arquitectura Monolítica

#### Estructura Actual

```
ui/
├── components/
│   └── HeaderPanel.java (único componente reutilizable)
├── projects/
│   ├── HomePanel.java
│   ├── ProyectosListPanel.java (~340 líneas)
│   ├── ProyectoDetailPanel.java (~280 líneas)
│   ├── ProyectoDialog.java (~210 líneas)
│   ├── IncidenciaDialog.java (~260 líneas)
│   └── ProyectoMetricasPanel.java (~244 líneas)
├── incidencias/
│   └── IncidenciaDetailPanel.java (~895 líneas - ¡MUY GRANDE!)
└── users/
    ├── UsuariosListPanel.java (~244 líneas)
    ├── MiPerfilPanel.java (~122 líneas)
    └── UpdateUsuarioDialog.java (~232 líneas)
```

### Problemas Identificados

1. **Paneles Monolíticos**:
   - `IncidenciaDetailPanel.java` tiene **895 líneas** - demasiado código en un solo archivo
   - Mezcla múltiples responsabilidades: UI, lógica de negocio, manejo de eventos, carga de datos

2. **Falta de Separación de Responsabilidades**:
   - Los paneles manejan directamente la lógica de negocio (llamadas a controllers)
   - La UI y la lógica están acopladas
   - Ejemplo: `IncidenciaDetailPanel` tiene métodos como `onResponsableChanged()`, `onEstadoChanged()`, `loadHistorial()`, etc.

3. **Duplicación de Código**:
   - Cada panel que muestra una tabla repite la lógica de `TableModel`, `ActionsRenderer`, `ActionsEditor`
   - Los `SwingWorker` se repiten con la misma estructura en múltiples lugares
   - El manejo de errores se duplica en cada panel

4. **Componentes No Reutilizables**:
   - Solo `HeaderPanel` está en la carpeta `components`
   - No hay componentes reutilizables para tablas, formularios, botones de acción, etc.

5. **Falta de Abstracción**:
   - No hay interfaces o clases base para paneles similares
   - Cada panel implementa su propia lógica desde cero

### 💡 Recomendaciones de Mejora

#### 1. Crear Componentes Reutilizables

```
components/
├── tables/
│   ├── ActionButtonsRenderer.java
│   ├── ActionButtonsEditor.java
│   └── BaseTablePanel.java (clase base para tablas)
├── forms/
│   ├── FormField.java (campo de formulario genérico)
│   └── FormDialog.java (diálogo de formulario base)
├── buttons/
│   ├── ActionButton.java (botón de acción reutilizable)
│   └── ButtonGroup.java (grupo de botones)
└── dialogs/
    ├── ConfirmDialog.java (diálogo de confirmación)
    └── ErrorDialog.java (diálogo de error)
```

#### 2. Separar Responsabilidades

**Antes** (monolítico):
```java
public class IncidenciaDetailPanel extends JPanel {
    // 895 líneas mezclando UI, lógica, eventos, etc.
}
```

**Después** (separado):
```java
// IncidenciaDetailPanel.java (orquestador)
public class IncidenciaDetailPanel extends JPanel {
    private IncidenciaDetailViewModel viewModel;
    private IncidenciaDetailView view;
    
    public IncidenciaDetailPanel(...) {
        viewModel = new IncidenciaDetailViewModel(controller);
        view = new IncidenciaDetailView(viewModel);
        add(view);
    }
}

// IncidenciaDetailView.java (solo UI)
public class IncidenciaDetailView extends JPanel {
    // Solo componentes visuales
}

// IncidenciaDetailViewModel.java (lógica)
public class IncidenciaDetailViewModel {
    // Lógica de negocio, carga de datos, etc.
}
```

#### 3. Crear Clases Base

```java
// BaseDetailPanel.java
public abstract class BaseDetailPanel<T> extends JPanel {
    protected abstract void loadEntity(Long id);
    protected abstract void buildUI();
    protected abstract void handleError(Exception ex);
}

// BaseListPanel.java
public abstract class BaseListPanel<T> extends JPanel {
    protected abstract void loadData();
    protected abstract TableModel createTableModel(List<T> data);
    protected abstract void onEdit(T entity);
    protected abstract void onView(T entity);
    protected abstract void onDelete(T entity);
}
```

#### 4. Extraer Lógica Común

```java
// SwingWorkerFactory.java -> PARA MANEJO DE ERRORES
public class SwingWorkerFactory {
    public static <T> SwingWorker<T, Void> create(
        Supplier<T> backgroundTask,
        Consumer<T> onSuccess,
        Consumer<Exception> onError
    ) {
        return new SwingWorker<T, Void>() {
            private Exception error;
            
            @Override
            protected T doInBackground() {
                try {
                    return backgroundTask.get();
                } catch (Exception ex) {
                    this.error = ex;
                    return null;
                }
            }
            
            @Override
            protected void done() {
                if (error != null) {
                    onError.accept(error);
                } else {
                    try {
                        onSuccess.accept(get());
                    } catch (Exception e) {
                        onError.accept(e);
                    }
                }
            }
        };
    }
}
```

#### 5. Beneficios de la Refactorización

- **Mantenibilidad**: Código más fácil de mantener y modificar
- **Reutilización**: Componentes que se pueden usar en múltiples lugares
- **Testabilidad**: Lógica separada de UI facilita las pruebas
- **Escalabilidad**: Más fácil agregar nuevas funcionalidades
- **Consistencia**: UI y comportamiento consistentes en toda la aplicación

---

## Resumen

### Backend
- ✅ Excepciones bien definidas con jerarquía clara
- ⚠️ Falta capa de manejo centralizado
- ⚠️ Duplicación de código en manejo de errores

### Frontend - Botones
- ❌ No hay reutilización de botones
- ❌ Cada panel crea sus propios botones
- ⚠️ Duplicación de código significativa

### Frontend - Arquitectura
- ⚠️ Paneles monolíticos (especialmente `IncidenciaDetailPanel`)
- ⚠️ Falta de separación de responsabilidades
- ⚠️ Poca reutilización de componentes
- ⚠️ Duplicación de código en tablas y formularios

### Prioridades de Mejora
1. **Alta**: Crear componentes reutilizables para botones de acción
2. **Alta**: Refactorizar `IncidenciaDetailPanel` (895 líneas)
3. **Media**: Crear ExceptionHandler centralizado
4. **Media**: Crear clases base para paneles similares
5. **Baja**: Extraer lógica común de SwingWorker
