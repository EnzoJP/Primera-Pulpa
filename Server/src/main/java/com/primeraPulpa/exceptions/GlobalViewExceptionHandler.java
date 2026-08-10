package com.primeraPulpa.exceptions;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.servlet.NoHandlerFoundException;

@ControllerAdvice
public class GlobalViewExceptionHandler {

    @ExceptionHandler(NoHandlerFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public String handleNotFound(NoHandlerFoundException ex, Model model, HttpServletRequest request) {
        model.addAttribute("error", "No se encontro la pagina solicitada");
        model.addAttribute("path", request.getRequestURI());
        return "error/404";
    }

    @ExceptionHandler(ErrorServiceException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public String handleBusinessError(ErrorServiceException ex, Model model, HttpServletRequest request) {
        model.addAttribute("error", ex.getMessage());
        model.addAttribute("path", request.getRequestURI());
        return "error/500";
    }

    @ExceptionHandler(Exception.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public String handleGenericError(Exception ex, Model model, HttpServletRequest request) {
        model.addAttribute("error", "Ocurrio un error interno");
        model.addAttribute("path", request.getRequestURI());
        return "error/500";
    }
}

