/**
 * Abrevia un identificador para mostrarlo en tablas: deja los primeros
 * `length` caracteres y agrega puntos suspensivos. El valor completo se
 * conserva para el atributo `title` de la celda.
 */
export function shortId(value: string | null | undefined, length = 8): string {
  if (!value) {
    return '';
  }
  return value.length > length ? `${value.slice(0, length)}…` : value;
}
