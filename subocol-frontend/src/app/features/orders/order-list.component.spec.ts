import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';

import { OrderListComponent } from './order-list.component';

describe('OrderListComponent', () => {
  let fixture: ComponentFixture<OrderListComponent>;
  let component: OrderListComponent;
  let httpMock: HttpTestingController;

  const orders = [
    {
      orderId: '3f9d1c2e-0000-4000-8000-000000000001',
      customerName: 'Ana Ruiz',
      vehiclePlate: 'ABC123',
      vehicleDescription: 'Toyota Corolla 2018',
      insurerCode: null,
      status: 'RECIBIDA',
      createdAt: '2026-11-11T15:04:05.123Z',
    },
  ];

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [OrderListComponent],
      providers: [provideRouter([]), provideHttpClient(), provideHttpClientTesting()],
    }).compileComponents();

    fixture = TestBed.createComponent(OrderListComponent);
    component = fixture.componentInstance;
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    httpMock.verify();
  });

  it('muestra el estado de carga mientras consulta el backend', () => {
    fixture.detectChanges();

    expect(component.loading()).toBe(true);
    expect(fixture.nativeElement.textContent).toContain('Cargando órdenes');

    httpMock
      .expectOne((request) => request.url === '/api/orders')
      .flush({
        content: orders,
        page: 0,
        size: 20,
        totalElements: 1,
      });
  });

  it('muestra el estado vacío cuando no hay órdenes registradas', () => {
    fixture.detectChanges();

    httpMock
      .expectOne((request) => request.url === '/api/orders')
      .flush({
        content: [],
        page: 0,
        size: 20,
        totalElements: 0,
      });
    fixture.detectChanges();

    expect(component.loading()).toBe(false);
    expect(fixture.nativeElement.textContent).toContain('Todavía no hay órdenes registradas');
  });

  it('lista las órdenes recibidas', () => {
    fixture.detectChanges();

    httpMock
      .expectOne((request) => request.url === '/api/orders')
      .flush({
        content: orders,
        page: 0,
        size: 20,
        totalElements: 1,
      });
    fixture.detectChanges();

    const rows = fixture.nativeElement.querySelectorAll('tbody tr');
    expect(rows.length).toBe(1);
    expect(rows[0].textContent).toContain('Ana Ruiz');
    expect(rows[0].textContent).toContain('ABC123');
    expect(rows[0].textContent).toContain('RECIBIDA');
  });

  it('corta el identificador de la orden en la tabla', () => {
    fixture.detectChanges();

    httpMock
      .expectOne((request) => request.url === '/api/orders')
      .flush({
        content: orders,
        page: 0,
        size: 20,
        totalElements: 1,
      });
    fixture.detectChanges();

    expect(component.shortOrderId(orders[0].orderId)).toBe('3f9d1c2e…');
    expect(fixture.nativeElement.querySelector('.table__id')?.textContent).toContain('3f9d1c2e');
  });

  it('muestra un error visible cuando el backend no responde', () => {
    fixture.detectChanges();

    httpMock
      .expectOne((request) => request.url === '/api/orders')
      .error(new ProgressEvent('error'), {
        status: 503,
        statusText: 'Service Unavailable',
      });
    fixture.detectChanges();

    expect(component.errorMessage()).not.toBeNull();
    expect(fixture.nativeElement.textContent).toContain('No se pudieron cargar las órdenes');
    expect(fixture.nativeElement.querySelector('[role="alert"]')).toBeTruthy();
  });

  it('muestra un error visible cuando la conexión falla', () => {
    fixture.detectChanges();

    httpMock
      .expectOne((request) => request.url === '/api/orders')
      .error(new ProgressEvent('error'), { status: 0 });
    fixture.detectChanges();

    expect(component.errorMessage()).toContain('No se pudo conectar con el servidor');
  });
});
