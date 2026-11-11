package com.subocol.gateway.client;

import java.net.ConnectException;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;
import java.net.http.HttpTimeoutException;
import java.util.concurrent.TimeoutException;

import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;

/**
 * Translates a failure of the destination service into the {@code ProblemDetail} that the
 * gateway returns to the caller.
 *
 * <p>
 * The contract is narrow on purpose: a destination that cannot be reached is a
 * {@code 502 Bad Gateway}, and a destination that does not answer in time is a
 * {@code 504 Gateway Timeout}. The internal traces never reach the caller: they stay in the
 * server log, correlated by the correlation id.
 */
public class UpstreamFailureClassifier {

	private static final String TIMEOUT_ON_BLOCKING_READ = "Timeout on blocking read";

	/**
	 * Classifies a failure raised while calling a destination service.
	 *
	 * @param error the raised failure
	 * @return the problem to return to the caller
	 */
	public ProblemDetail classify(Throwable error) {
		if (isTimeout(error)) {
			ProblemDetail problem = ProblemDetail.forStatus(HttpStatus.GATEWAY_TIMEOUT);
			problem.setTitle("Tiempo de espera agotado");
			problem.setDetail("El servicio de destino no respondió dentro del tiempo configurado.");
			return problem;
		}
		ProblemDetail problem = ProblemDetail.forStatus(HttpStatus.BAD_GATEWAY);
		problem.setTitle("Servicio de destino no disponible");
		problem.setDetail(isConnectionFailure(error)
				? "El gateway no pudo establecer conexión con el servicio de destino."
				: "El gateway no pudo obtener una respuesta válida del servicio de destino.");
		return problem;
	}

	/**
	 * Tells whether the failure was caused by an expired timeout.
	 *
	 * @param error the raised failure
	 * @return {@code true} when the destination service did not answer in time
	 */
	boolean isTimeout(Throwable error) {
		for (Throwable current = error; current != null; current = current.getCause()) {
			if (current instanceof HttpTimeoutException || current instanceof SocketTimeoutException
					|| current instanceof TimeoutException) {
				return true;
			}
			if (current instanceof IllegalStateException state && state.getMessage() != null
					&& state.getMessage().contains(TIMEOUT_ON_BLOCKING_READ)) {
				return true;
			}
			if (current.getCause() == current) {
				break;
			}
		}
		return false;
	}

	/**
	 * Tells whether the destination service could not be reached at all.
	 *
	 * @param error the raised failure
	 * @return {@code true} when the connection failed
	 */
	boolean isConnectionFailure(Throwable error) {
		for (Throwable current = error; current != null; current = current.getCause()) {
			if (current instanceof ConnectException || current instanceof UnknownHostException
					|| current instanceof SocketException) {
				return true;
			}
			if (current.getCause() == current) {
				break;
			}
		}
		return false;
	}

}
