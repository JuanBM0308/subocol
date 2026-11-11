import { DatePipe } from '@angular/common';
import { HttpErrorResponse } from '@angular/common/http';
import { Component, DestroyRef, inject, signal } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { ActivatedRoute, RouterLink } from '@angular/router';
import { distinctUntilChanged, map } from 'rxjs';

import { describeApiError } from '../../core/api-error';
import { OrderDetail } from '../../core/order.models';
import { OrderService } from '../../core/order.service';
import { shortId } from '../../core/short-id';

/**
 * Detalle de una orden de servicio: sus datos y el historial completo de
 * cambios de estado.
 *
 * El historial es append-only en el backend: la tabla es de solo lectura y
 * cada fila muestra la cadena de hashes que respalda la trazabilidad.
 */
@Component({
  selector: 'app-order-detail',
  imports: [DatePipe, RouterLink],
  templateUrl: './order-detail.component.html',
})
export class OrderDetailComponent {
  private readonly orderService = inject(OrderService);
  private readonly route = inject(ActivatedRoute);
  private readonly destroyRef = inject(DestroyRef);

  /** Identificador de la orden que se está mostrando. */
  public readonly orderId = signal('');
  /** Detalle cargado, con su historial de estados. */
  public readonly order = signal<OrderDetail | null>(null);
  /** `true` mientras la consulta al backend está en vuelo. */
  public readonly loading = signal(true);
  /** Mensaje de error visible; `null` cuando no hay error. */
  public readonly errorMessage = signal<string | null>(null);
  /** `true` cuando la API respondió 404: la orden no existe. */
  public readonly notFound = signal(false);

  constructor() {
    this.route.paramMap
      .pipe(
        map((params) => params.get('id') ?? ''),
        distinctUntilChanged(),
        takeUntilDestroyed(this.destroyRef),
      )
      .subscribe((orderId) => this.loadOrder(orderId));
  }

  /** Carga (o recarga) el detalle de la orden indicada. */
  public loadOrder(orderId: string): void {
    this.orderId.set(orderId);
    this.loading.set(true);
    this.errorMessage.set(null);
    this.notFound.set(false);
    this.order.set(null);

    if (!orderId) {
      this.notFound.set(true);
      this.loading.set(false);
      return;
    }

    this.orderService
      .getOrder(orderId)
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: (detail) => {
          this.order.set(detail);
          this.loading.set(false);
        },
        error: (error: HttpErrorResponse) => {
          this.order.set(null);
          if (error.status === 404) {
            this.notFound.set(true);
          } else {
            this.errorMessage.set(describeApiError(error));
          }
          this.loading.set(false);
        },
      });
  }

  /** Reintenta la carga del detalle. */
  public reload(): void {
    this.loadOrder(this.orderId());
  }

  /** Identificador abreviado de la orden. */
  public shortOrderId(orderId: string): string {
    return shortId(orderId);
  }

  /** Hash del evento abreviado; el completo va en el `title`. */
  public shortHash(hash: string): string {
    return shortId(hash);
  }
}
