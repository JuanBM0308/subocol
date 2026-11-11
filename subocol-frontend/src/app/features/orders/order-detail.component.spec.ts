import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { ActivatedRoute, convertToParamMap, provideRouter } from '@angular/router';
import { RouterTestingHarness } from '@angular/router/testing';
import { of } from 'rxjs';

import { OrderDetail } from '../../core/order.models';
import { routes } from '../../app.routes';
import { OrderDetailComponent } from './order-detail.component';

describe('OrderDetailComponent', () => {
  const ORDER_ID = '3f9d1c2e-0000-4000-8000-000000000001';

  const detail: OrderDetail = {
    orderId: ORDER_ID,
    customerName: 'Ana Ruiz',
    vehiclePlate: 'ABC123',
    vehicleDescription: 'Toyota Corolla 2018',
    insurerCode: 'SUR-001',
    status: 'RECIBIDA',
    createdAt: '2026-11-11T15:04:05.123Z',
    history: [
      {
        eventId: '9c1a5f70-0000-4000-8000-000000000002',
        fromStatus: null,
        toStatus: 'RECIBIDA',
        reason: 'Orden registrada',
        changedBy: 'dev-advisor',
        changedAt: '2026-11-11T15:04:05.123Z',
        eventHash: '9f2b1c3d4e5f60718293a4b5c6d7e8f9',
        previousHash: '0000000000000000000000000000000000000000000000000000000000000000',
      },
    ],
  };

  let fixture: ComponentFixture<OrderDetailComponent>;
  let component: OrderDetailComponent;
  let httpMock: HttpTestingController;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [OrderDetailComponent],
      providers: [
        provideRouter([]),
        provideHttpClient(),
        provideHttpClientTesting(),
        {
          provide: ActivatedRoute,
          useValue: { paramMap: of(convertToParamMap({ id: ORDER_ID })) },
        },
      ],
    }).compileComponents();

    fixture = TestBed.createComponent(OrderDetailComponent);
    component = fixture.componentInstance;
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    httpMock.verify();
  });

  it('muestra el estado de carga mientras consulta el backend', () => {
    fixture.detectChanges();

    expect(component.loading()).toBe(true);
    expect(fixture.nativeElement.textContent).toContain('Cargando la orden');

    httpMock.expectOne(`/api/orders/${ORDER_ID}`).flush(detail);
  });

  it('pide al backend la orden indicada en la ruta', () => {
    fixture.detectChanges();

    const request = httpMock.expectOne(`/api/orders/${ORDER_ID}`);
    expect(request.request.method).toBe('GET');
    request.flush(detail);
  });

  it('muestra los datos de la orden', () => {
    fixture.detectChanges();

    httpMock.expectOne(`/api/orders/${ORDER_ID}`).flush(detail);
    fixture.detectChanges();

    const text = fixture.nativeElement.textContent as string;
    expect(text).toContain('Ana Ruiz');
    expect(text).toContain('ABC123');
    expect(text).toContain('Toyota Corolla 2018');
    expect(text).toContain('SUR-001');
    expect(text).toContain('RECIBIDA');
    expect(text).toContain(ORDER_ID);
  });

  it('muestra el historial de estados con su trazabilidad', () => {
    fixture.detectChanges();

    httpMock.expectOne(`/api/orders/${ORDER_ID}`).flush(detail);
    fixture.detectChanges();

    const rows = fixture.nativeElement.querySelectorAll('tbody tr');
    expect(rows.length).toBe(1);

    const row = rows[0].textContent as string;
    expect(row).toContain('Inicial');
    expect(row).toContain('RECIBIDA');
    expect(row).toContain('Orden registrada');
    expect(row).toContain('dev-advisor');
    expect(row).toContain('9f2b1c3d…');
  });

  it('informa cuando la orden todavía no tiene historial', () => {
    fixture.detectChanges();

    httpMock.expectOne(`/api/orders/${ORDER_ID}`).flush({ ...detail, history: [] });
    fixture.detectChanges();

    expect(fixture.nativeElement.textContent).toContain(
      'Todavía no hay cambios de estado registrados',
    );
  });

  it('muestra orden no encontrada cuando la API responde 404', () => {
    fixture.detectChanges();

    httpMock
      .expectOne(`/api/orders/${ORDER_ID}`)
      .flush(
        { title: 'Not Found', detail: 'No existe la orden solicitada' },
        { status: 404, statusText: 'Not Found' },
      );
    fixture.detectChanges();

    expect(component.notFound()).toBe(true);
    expect(component.errorMessage()).toBeNull();
    expect(fixture.nativeElement.textContent).toContain('Orden no encontrada');
  });

  it('muestra un error visible cuando la conexión falla', () => {
    fixture.detectChanges();

    httpMock.expectOne(`/api/orders/${ORDER_ID}`).error(new ProgressEvent('error'), { status: 0 });
    fixture.detectChanges();

    expect(component.errorMessage()).toContain('No se pudo conectar con el servidor');
    expect(fixture.nativeElement.textContent).toContain('No se pudo cargar la orden');
    expect(fixture.nativeElement.querySelector('[role="alert"]')).toBeTruthy();
  });

  it('permite reintentar la carga de la orden', () => {
    fixture.detectChanges();

    httpMock
      .expectOne(`/api/orders/${ORDER_ID}`)
      .error(new ProgressEvent('error'), { status: 503, statusText: 'Service Unavailable' });
    fixture.detectChanges();

    const retry = fixture.nativeElement.querySelector('button') as HTMLButtonElement;
    expect(retry.textContent).toContain('Reintentar');
    retry.click();
    fixture.detectChanges();

    httpMock.expectOne(`/api/orders/${ORDER_ID}`).flush(detail);
    fixture.detectChanges();

    expect(component.loading()).toBe(false);
    expect(component.errorMessage()).toBeNull();
    expect(fixture.nativeElement.textContent).toContain('Ana Ruiz');
  });
});

describe('OrderDetailComponent en la ruta detalle/:id', () => {
  const ORDER_ID = '3f9d1c2e-0000-4000-8000-000000000001';

  it('se monta en la ruta detalle/:id y carga la orden', async () => {
    await TestBed.configureTestingModule({
      providers: [provideRouter(routes), provideHttpClient(), provideHttpClientTesting()],
    }).compileComponents();

    const httpMock = TestBed.inject(HttpTestingController);
    const harness = await RouterTestingHarness.create();

    await harness.navigateByUrl(`/detalle/${ORDER_ID}`);

    const request = httpMock.expectOne(`/api/orders/${ORDER_ID}`);
    expect(request.request.method).toBe('GET');
    request.flush({
      orderId: ORDER_ID,
      customerName: 'Ana Ruiz',
      vehiclePlate: 'ABC123',
      vehicleDescription: 'Toyota Corolla 2018',
      insurerCode: null,
      status: 'RECIBIDA',
      createdAt: '2026-11-11T15:04:05.123Z',
      history: [],
    });
    harness.detectChanges();

    expect(harness.routeNativeElement?.textContent).toContain('Ana Ruiz');
    expect(harness.routeNativeElement?.textContent).toContain('Sin aseguradora');

    httpMock.verify();
  });
});
