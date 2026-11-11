import { DatePipe } from '@angular/common';
import { HttpErrorResponse } from '@angular/common/http';
import { Component, DestroyRef, inject, signal } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { RouterLink } from '@angular/router';

import { describeApiError } from '../../core/api-error';
import { Order } from '../../core/order.models';
import { OrderService } from '../../core/order.service';
import { shortId } from '../../core/short-id';

/**
 * Listado de órdenes de servicio.
 *
 * Cubre los tres estados visibles del listado: cargando, con error y con datos.
 * Si el backend no responde, el error queda en pantalla: nunca una pantalla
 * vacía en silencio (FR7.8).
 */
@Component({
  selector: 'app-order-list',
  imports: [DatePipe, RouterLink],
  templateUrl: './order-list.component.html',
})
export class OrderListComponent {
  private readonly orderService = inject(OrderService);
  private readonly destroyRef = inject(DestroyRef);

  /** Órdenes de la página actual. */
  public readonly orders = signal<Order[]>([]);
  /** `true` mientras la consulta al backend está en vuelo. */
  public readonly loading = signal(true);
  /** Mensaje de error visible; `null` cuando no hay error. */
  public readonly errorMessage = signal<string | null>(null);
  /** Total de órdenes informadas por el backend. */
  public readonly totalElements = signal(0);

  constructor() {
    this.loadOrders();
  }

  /** Vuelve a consultar el listado. */
  public loadOrders(): void {
    this.loading.set(true);
    this.errorMessage.set(null);

    this.orderService
      .listOrders(0, 20)
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: (page) => {
          this.orders.set(page?.content ?? []);
          this.totalElements.set(page?.totalElements ?? 0);
          this.loading.set(false);
        },
        error: (error: HttpErrorResponse) => {
          this.orders.set([]);
          this.totalElements.set(0);
          this.errorMessage.set(describeApiError(error));
          this.loading.set(false);
        },
      });
  }

  /** Identificador abreviado para mostrar en la tabla; el completo va en el title. */
  public shortOrderId(orderId: string): string {
    return shortId(orderId);
  }
}
