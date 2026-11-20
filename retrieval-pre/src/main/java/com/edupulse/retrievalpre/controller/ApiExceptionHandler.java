package com.edupulse.retrievalpre.controller;

import com.edupulse.retrievalpre.filter.InvalidFilterException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * Turns API errors into RFC 7807 problem details, so clients get a clear, structured message.
 */
@RestControllerAdvice
public class ApiExceptionHandler {

    static final String FILTER_EXAMPLE = "source == 'owasp-top10-2021.pdf' && page > 3";

    @ExceptionHandler(InvalidFilterException.class)
    public ProblemDetail handleInvalidFilter(InvalidFilterException e) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, e.getMessage());
        problem.setTitle("Invalid filter");
        problem.setProperty("validFields", e.getValidFields());
        problem.setProperty("validOperators", e.getValidOperators());
        problem.setProperty("example", FILTER_EXAMPLE);
        return problem;
    }
}
