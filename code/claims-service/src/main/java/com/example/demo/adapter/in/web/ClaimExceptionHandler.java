package com.example.demo.adapter.in.web;

import com.example.demo.application.exceptions.UnauthorizedClaimAccessException;
import com.example.demo.application.exceptions.UserNotFoundException;
import com.example.demo.domain.claim.ClaimNotFoundException;
import com.example.demo.domain.claim.InvalidClaimException;
import com.example.demo.domain.claim.InvalidStatusTransitionException;
import com.example.demo.user.domain.UnauthorizedException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class ClaimExceptionHandler {

    private final ErrorTypeConstants errorTypes;

    public ClaimExceptionHandler(ErrorTypeConstants errorTypes) {
        this.errorTypes = errorTypes;
    }

    @ExceptionHandler(UserNotFoundException.class)
    public ProblemDetail handleUserNotFound(UserNotFoundException exception) {
        ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(
                HttpStatus.BAD_REQUEST, exception.getMessage());
        problemDetail.setTitle("Validation Error");
        problemDetail.setType(errorTypes.validationError());
        return problemDetail;
    }

    @ExceptionHandler(InvalidClaimException.class)
    public ProblemDetail handleInvalidClaim(InvalidClaimException exception) {
        ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(
                HttpStatus.BAD_REQUEST, exception.getMessage());
        problemDetail.setTitle("Validation Error");
        problemDetail.setType(errorTypes.validationError());
        return problemDetail;
    }

    @ExceptionHandler(ClaimNotFoundException.class)
    public ProblemDetail handleClaimNotFound(ClaimNotFoundException exception) {
        ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(
                HttpStatus.NOT_FOUND, exception.getMessage());
        problemDetail.setTitle("Not Found");
        problemDetail.setType(errorTypes.notFound());
        return problemDetail;
    }

    @ExceptionHandler(UnauthorizedClaimAccessException.class)
    public ProblemDetail handleUnauthorizedClaimAccess(UnauthorizedClaimAccessException exception) {
        ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(
                HttpStatus.FORBIDDEN, exception.getMessage());
        problemDetail.setTitle("Forbidden");
        problemDetail.setType(errorTypes.forbidden());
        return problemDetail;
    }

    // was getting 500 when trying to move a claim to an invalid state e.g CLOSED -> APPROVED
    // should return 422 not 500 — adding handler for this
    @ExceptionHandler(InvalidStatusTransitionException.class)
    public ProblemDetail handleInvalidStatusTransition(InvalidStatusTransitionException e) {
        ProblemDetail pd = ProblemDetail.forStatusAndDetail(
                HttpStatus.UNPROCESSABLE_ENTITY, e.getMessage());
        pd.setTitle("Invalid Status Transition");
        pd.setType(errorTypes.validationError());
        return pd;
    }

    // same issue — non admin user trying to update claim status was returning 500
    // UnauthorizedException is a different class than UnauthorizedClaimAccessException
    // need a separate handler for it
    @ExceptionHandler(UnauthorizedException.class)
    public ProblemDetail handleUnauthorized(UnauthorizedException e) {
        ProblemDetail pd = ProblemDetail.forStatusAndDetail(
                HttpStatus.FORBIDDEN, e.getMessage());
        pd.setTitle("Forbidden");
        pd.setType(errorTypes.forbidden());
        return pd;
    }
}
