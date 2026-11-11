import { HttpClient, HttpParams } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';

import { CreateOrderRequest, OrderCreated, OrderDetail, OrderPage } from './order.models';

/**
 * Cliente de la API de órdenes.
 *
 * Las rutas son relativas a propósito: nginx sirve la aplicación y hace de
 * proxy inverso hacia el gateway, así que el navegador nunca ve otro origen.
 */
@Injectable({ providedIn: 'root' })
export class OrderService {
  private readonly http = inject(HttpClient);
  private readonly baseUrl = '/api/orders';

  /** Lista paginada de órdenes. */
  listOrders(page = 0, size = 20): Observable<OrderPage> {
    const params = new HttpParams().set('page', page).set('size', size);
    return this.http.get<OrderPage>(this.baseUrl, { params });
  }

  /** Registra una orden nueva y devuelve el identificador asignado por el backend. */
  createOrder(request: CreateOrderRequest): Observable<OrderCreated> {
    return this.http.post<OrderCreated>(this.baseUrl, request);
  }

  /** Detalle de una orden, con su historial de estados. */
  getOrder(orderId: string): Observable<OrderDetail> {
    return this.http.get<OrderDetail>(`${this.baseUrl}/${encodeURIComponent(orderId)}`);
  }
}
