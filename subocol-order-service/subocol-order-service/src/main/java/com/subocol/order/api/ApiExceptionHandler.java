package com.subocol.order.api;

import java.net.URI;
import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import com.subocol.order.domain.OrderNotFoundException;
import com.subocol.order.service.InvalidPageRequestException;

/**
 * Errores de la API de órdenes.
 *
 * <p>Todos los errores de validación y de negocio salen como
 * {@link ProblemDetail} y sin trazas internas: el cliente recibe qué falló,
 * no la pila de la aplicación.
 */
@RestControllerAdvice
public class ApiExceptionHandler {

	private static final String PROBLEM_BASE = "urn:subocol:problem:";

	@ExceptionHandler(MethodArgumentNotValidException.class)
	public ResponseEntity<ProblemDetail> handleValidation(MethodArgumentNotValidException ex) {
		List<FieldError> errors = ex.getBindingResult().getFieldErrors().stream()
				.map(error -> new FieldError(error.getField(), error.getDefaultMessage()))
				.toList();
		ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST,
				"The request contains invalid or missing values.");
		problem.setTitle("Validation failed");
		problem.setType(URI.create(PROBLEM_BASE + "validation-failed"));
		problem.setProperty("errors", errors);
		return ResponseEntity.badRequest().body(problem);
	}

	@ExceptionHandler(OrderNotFoundException.class)
	public ResponseEntity<ProblemDetail> handleNotFound(OrderNotFoundException ex) {
		ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND,
				"The requested order does not exist.");
		problem.setTitle("Order not found");
		problem.setType(URI.create(PROBLEM_BASE + "order-not-found"));
		problem.setProperty("orderId", ex.getOrderId());
		return ResponseEntity.status(HttpStatus.NOT_FOUND).body(problem);
	}

	@ExceptionHandler(InvalidPageRequestException.class)
	public ResponseEntity<ProblemDetail> handleInvalidPagination(InvalidPageRequestException ex) {
		ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, ex.getMessage());
		problem.setTitle("Invalid pagination");
		problem.setType(URI.create(PROBLEM_BASE + "invalid-pagination"));
		return ResponseEntity.badRequest().body(problem);
	}

	@ExceptionHandler(MethodArgumentTypeMismatchException.class)
	public ResponseEntity<ProblemDetail> handleTypeMismatch(MethodArgumentTypeMismatchException ex) {
		ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST,
				"The request contains a parameter with an invalid format.");
		problem.setTitle("Invalid request parameter");
		problem.setType(URI.create(PROBLEM_BASE + "invalid-parameter"));
		return ResponseEntity.badRequest().body(problem);
	}

	@ExceptionHandler(HttpMessageNotReadableException.class)
	public ResponseEntity<ProblemDetail> handleUnreadableBody(HttpMessageNotReadableException ex) {
		ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST,
				"The request body could not be read.");
		problem.setTitle("Malformed request body");
		problem.setType(URI.create(PROBLEM_BASE + "malformed-body"));
		return ResponseEntity.badRequest().body(problem);
	}

	/**
	 * Detalle de un campo que no pasó la validación.
	 *
	 * @param field nombre del campo
	 * @param message motivo del rechazo
	 */
	public record FieldError(String field, String message) {

	}

}
