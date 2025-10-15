package ar.edu.up.bugtracker.exceptions;

public class DaoException extends AppException {
    public DaoException(String message) { super(message); }
    public DaoException(String message, Throwable cause) { super(message, cause); }
}
