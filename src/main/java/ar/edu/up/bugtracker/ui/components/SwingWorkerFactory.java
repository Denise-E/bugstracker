package ar.edu.up.bugtracker.ui.components;

import javax.swing.*;
import java.util.function.Consumer;
import java.util.function.Supplier;

// Factory para crear SwingWorkers con manejo de errores centralizado.
public class SwingWorkerFactory {
    
    /**
     * Crea un SwingWorker con manejo de errores centralizado.
     * 
     * @param <T> Tipo del resultado de la tarea en background
     * @param backgroundTask Tarea a ejecutar en background (doInBackground)
     * @param onSuccess Callback a ejecutar cuando la tarea es exitosa (recibe el resultado)
     * @param onError Callback a ejecutar cuando ocurre un error (recibe la excepción)
     * @return SwingWorker configurado
     */
    public static <T> SwingWorker<T, Void> create(
            Supplier<T> backgroundTask,
            Consumer<T> onSuccess,
            Consumer<Exception> onError) {
        
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
                        T result = get();
                        onSuccess.accept(result);
                    } catch (Exception e) {
                        onError.accept(e);
                    }
                }
            }
        };
    }
    
    /**
     * Crea un SwingWorker que muestra errores automáticamente usando ExceptionHandler.
     * 
     * @param <T> Tipo del resultado de la tarea en background
     * @param parentComponent Componente padre para mostrar el diálogo de error
     * @param backgroundTask Tarea a ejecutar en background
     * @param onSuccess Callback a ejecutar cuando la tarea es exitosa
     * @return SwingWorker configurado con manejo automático de errores
     */
    public static <T> SwingWorker<T, Void> createWithAutoErrorHandling(
            java.awt.Component parentComponent,
            Supplier<T> backgroundTask,
            Consumer<T> onSuccess) {
        
        return create(
            backgroundTask,
            onSuccess,
            error -> {
                String message = ExceptionHandler.getErrorMessage(error);
                String title = ExceptionHandler.getErrorTitle(error);
                int messageType = ExceptionHandler.getMessageType(error);
                JOptionPane.showMessageDialog(parentComponent, message, title, messageType);
            }
        );
    }
    
    /**
     * Crea un SwingWorker para tareas que no retornan valor.
     * 
     * @param backgroundTask Tarea a ejecutar en background
     * @param onSuccess Callback a ejecutar cuando la tarea es exitosa
     * @param onError Callback a ejecutar cuando ocurre un error
     * @return SwingWorker configurado
     */
    public static SwingWorker<Void, Void> createVoid(
            Runnable backgroundTask,
            Runnable onSuccess,
            Consumer<Exception> onError) {
        
        return new SwingWorker<Void, Void>() {
            private Exception error;

            @Override
            protected Void doInBackground() {
                try {
                    backgroundTask.run();
                    return null;
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
                    onSuccess.run();
                }
            }
        };
    }
    
    /**
     * Crea un SwingWorker Void con manejo automático de errores.
     * 
     * @param parentComponent Componente padre para mostrar el diálogo de error
     * @param backgroundTask Tarea a ejecutar en background
     * @param onSuccess Callback a ejecutar cuando la tarea es exitosa
     * @return SwingWorker configurado con manejo automático de errores
     */
    public static SwingWorker<Void, Void> createVoidWithAutoErrorHandling(
            java.awt.Component parentComponent,
            Runnable backgroundTask,
            Runnable onSuccess) {
        
        return createVoid(
            backgroundTask,
            onSuccess,
            error -> {
                String message = ExceptionHandler.getErrorMessage(error);
                String title = ExceptionHandler.getErrorTitle(error);
                int messageType = ExceptionHandler.getMessageType(error);
                JOptionPane.showMessageDialog(parentComponent, message, title, messageType);
            }
        );
    }
}
