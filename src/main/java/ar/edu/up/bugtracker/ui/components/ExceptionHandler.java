package ar.edu.up.bugtracker.ui.components;

import ar.edu.up.bugtracker.exceptions.*;

import javax.swing.*;

// Manejador centralizado de excepciones para la UI.
public class ExceptionHandler {
    
    public static String getErrorMessage(Exception ex) {
        if (ex == null) {
            return "Ocurrió un error inesperado.";
        }
        
        if (ex instanceof ValidationException) {
            return ex.getMessage();
        } else if (ex instanceof NotFoundException) {
            return ex.getMessage() != null && !ex.getMessage().isEmpty() 
                ? ex.getMessage() 
                : "Recurso no encontrado.";
        } else if (ex instanceof ForbiddenException) {
            return "No tenés permisos para realizar esta acción.";
        } else if (ex instanceof AuthException) {
            return ex.getMessage() != null && !ex.getMessage().isEmpty()
                ? ex.getMessage()
                : "Debes estar autenticado para realizar esta acción.";
        } else if (ex instanceof BusinessException) {
            return ex.getMessage() != null && !ex.getMessage().isEmpty()
                ? ex.getMessage()
                : "Ocurrió un error en la operación.";
        } else if (ex instanceof AppException) {
            return ex.getMessage() != null && !ex.getMessage().isEmpty()
                ? ex.getMessage()
                : "Ocurrió un error inesperado.";
        } else {
            String message = ex.getMessage();
            return message != null && !message.isEmpty()
                ? "Error: " + message
                : "Ocurrió un error inesperado.";
        }
    }
    
    public static String getErrorTitle(Exception ex) {
        if (ex instanceof ValidationException) {
            return "Error de validación";
        } else if (ex instanceof NotFoundException) {
            return "No encontrado";
        } else if (ex instanceof ForbiddenException) {
            return "Acceso denegado";
        } else if (ex instanceof AuthException) {
            return "Autenticación requerida";
        } else {
            return "Error";
        }
    }
    
    //Obtiene el tipo de mensaje (JOptionPane) según el tipo de excepción.
    public static int getMessageType(Exception ex) {
        if (ex instanceof ValidationException) {
            return JOptionPane.WARNING_MESSAGE;
        } else if (ex instanceof ForbiddenException || ex instanceof AuthException) {
            return JOptionPane.WARNING_MESSAGE;
        } else {
            return JOptionPane.ERROR_MESSAGE;
        }
    }
}
