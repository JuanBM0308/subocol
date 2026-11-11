import { HttpErrorResponse } from '@angular/common/http';

/** Forma mínima de un `ProblemDetail` (RFC 9457) devuelto por el backend. */
interface ProblemDetail {
  title?: string;
  detail?: string;
  message?: string;
}

/**
 * Traduce un fallo de la API a un mensaje que el usuario pueda entender.
 * Un fallo de red (estado 0) se distingue de un rechazo del servidor: el
 * frontend nunca debe mostrar una pantalla vacía en silencio (FR7.8).
 */
export function describeApiError(error: unknown): string {
  if (!(error instanceof HttpErrorResponse)) {
    return 'Ocurrió un error inesperado al procesar la solicitud.';
  }

  if (error.status === 0) {
    return 'No se pudo conectar con el servidor. Verificá que el gateway esté disponible e intentá de nuevo.';
  }

  const body = error.error as ProblemDetail | string | null;

  if (body && typeof body === 'object') {
    const message = body.detail ?? body.title ?? body.message;
    if (message) {
      return message;
    }
  }

  if (typeof body === 'string' && body.trim().length > 0) {
    return body;
  }

  return `El servidor respondió con el estado ${error.status}.`;
}
