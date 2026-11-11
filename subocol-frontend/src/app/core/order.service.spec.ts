import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';

import { CreateOrderRequest, OrderCreated, OrderDetail, OrderPage } from './order.models';
import { OrderService } from './order.service';

describe('OrderService', () => {
  let service: OrderService;
  let httpMock: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [provideHttpClient(), provideHttpClientTesting()],
    });

    service = TestBed.inject(OrderService);
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    httpMock.verify();
  });

  it('lista las órdenes con page y size', () => {
    const page: OrderPage = {
      content: [
        {
          orderId: '3f9d1c2e-0000-4000-8000-000000000001',
          customerName: 'Ana Ruiz',
          vehiclePlate: 'ABC123',
          vehicleDescription: 'Toyota Corolla 2018',
          insurerCode: null,
          status: 'RECIBIDA',
          createdAt: '2026-11-11T15:04:05.123Z',
        },
      ],
      page: 0,
      size: 20,
      totalElements: 1,
    };

    let received: OrderPage | undefined;
    service.listOrders(0, 20).subscribe((result) => (received = result));

    const request = httpMock.expectOne(
      (candidate) =>
        candidate.url === '/api/orders' &&
        candidate.params.get('page') === '0' &&
        candidate.params.get('size') === '20',
    );
    expect(request.request.method).toBe('GET');
    request.flush(page);

    expect(received).toEqual(page);
  });

  it('usa por defecto la primera página de 20 órdenes', () => {
    service.listOrders().subscribe();

    const request = httpMock.expectOne(
      (candidate) =>
        candidate.url === '/api/orders' &&
        candidate.params.get('page') === '0' &&
        candidate.params.get('size') === '20',
    );
    expect(request.request.method).toBe('GET');
    request.flush({ content: [], page: 0, size: 20, totalElements: 0 });
  });

  it('crea una orden y devuelve el identificador asignado', () => {
    const payload: CreateOrderRequest = {
      customerName: 'Ana Ruiz',
      vehiclePlate: 'ABC123',
      vehicleDescription: 'Toyota Corolla 2018',
      insurerCode: null,
    };
    const created: OrderCreated = {
      orderId: '3f9d1c2e-0000-4000-8000-000000000001',
      status: 'RECIBIDA',
      createdAt: '2026-11-11T15:04:05.123Z',
    };

    let received: OrderCreated | undefined;
    service.createOrder(payload).subscribe((result) => (received = result));

    const request = httpMock.expectOne('/api/orders');
    expect(request.request.method).toBe('POST');
    expect(request.request.body).toEqual(payload);
    request.flush(created, { status: 201, statusText: 'Created' });

    expect(received).toEqual(created);
  });

  it('pide el detalle de una orden por identificador', () => {
    const detail: OrderDetail = {
      orderId: '3f9d1c2e-0000-4000-8000-000000000001',
      customerName: 'Ana Ruiz',
      vehiclePlate: 'ABC123',
      vehicleDescription: 'Toyota Corolla 2018',
      insurerCode: null,
      status: 'RECIBIDA',
      createdAt: '2026-11-11T15:04:05.123Z',
      history: [],
    };

    let received: OrderDetail | undefined;
    service.getOrder(detail.orderId).subscribe((result) => (received = result));

    const request = httpMock.expectOne('/api/orders/3f9d1c2e-0000-4000-8000-000000000001');
    expect(request.request.method).toBe('GET');
    request.flush(detail);

    expect(received).toEqual(detail);
  });
});
