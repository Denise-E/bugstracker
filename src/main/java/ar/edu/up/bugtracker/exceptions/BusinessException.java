package ar.edu.up.bugtracker.exceptions;

public class BusinessException extends AppException {
    public BusinessException(String message) { super(message); }
    public BusinessException(String message, Throwable cause) { super(message, cause); }
}
