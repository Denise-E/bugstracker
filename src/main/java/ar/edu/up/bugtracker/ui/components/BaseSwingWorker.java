package ar.edu.up.bugtracker.ui.components;

import javax.swing.*;

/**
 * Clase base abstracta para SwingWorkers con manejo de errores centralizado.
 *  * 
 * @param <T> Tipo del resultado de la tarea en background
 * @param <V> Tipo de los valores intermedios (usualmente Void)
 */
public abstract class BaseSwingWorker<T, V> extends SwingWorker<T, V> {
    protected Exception error;
    protected final java.awt.Component parentComponent;

    /**
     * Constructor con componente padre para mostrar diálogos de error.
     * 
     * @param parentComponent Componente padre para mostrar diálogos de error
     */
    public BaseSwingWorker(java.awt.Component parentComponent) {
        this.parentComponent = parentComponent;
    }

    /**
     * Constructor sin componente padre (los errores se mostrarán sin diálogo padre).
     */
    public BaseSwingWorker() {
        this.parentComponent = null;
    }

    @Override
    protected final T doInBackground() {
        try {
            return doInBackgroundImpl();
        } catch (Exception ex) {
            this.error = ex;
            return null;
        }
    }

    /**
     * Implementación de la tarea en background.
     * Las subclases deben implementar este método en lugar de doInBackground().
     * 
     * @return Resultado de la tarea
     * @throws Exception Si ocurre un error durante la ejecución
     */
    protected abstract T doInBackgroundImpl() throws Exception;

    @Override
    protected final void done() {
        if (error != null) {
            handleError(error);
        } else {
            try {
                T result = get();
                onSuccess(result);
            } catch (Exception e) {
                handleError(e);
            }
        }
    }

    /**
     * Método llamado cuando la tarea se completa exitosamente.
     * Las subclases deben implementar este método.
     * 
     * @param result Resultado de la tarea
     */
    protected abstract void onSuccess(T result);

    /**
     * Maneja errores que ocurren durante la ejecución.
     * Por defecto muestra un diálogo con el mensaje de error.
     * Las subclases pueden sobrescribir este método para personalizar el manejo.
     * 
     * @param error La excepción que ocurrió
     */
    protected void handleError(Exception error) {
        String message = ExceptionHandler.getErrorMessage(error);
        String title = ExceptionHandler.getErrorTitle(error);
        int messageType = ExceptionHandler.getMessageType(error);
        JOptionPane.showMessageDialog(parentComponent, message, title, messageType);
    }
}
