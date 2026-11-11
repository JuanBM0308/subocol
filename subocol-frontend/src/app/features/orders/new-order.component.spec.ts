import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideRouter, Router } from '@angular/router';

import { NewOrderComponent } from './new-order.component';

describe('NewOrderComponent', () => {
  let fixture: ComponentFixture<NewOrderComponent>;
  let component: NewOrderComponent;
  let httpMock: HttpTestingController;
  let router: Router;

  const fillValidOrder = () => {
    component.form.controls.customerName.setValue('Ana Ruiz');
    component.form.controls.vehiclePlate.setValue('ABC123');
    component.form.controls.vehicleDescription.setValue('Toyota Corolla 2018');
    component.form.controls.insurerCode.setValue('');
  };

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [NewOrderComponent],
      providers: [provideRouter([]), provideHttpClient(), provideHttpClientTesting()],
    }).compileComponents();

    fixture = TestBed.createComponent(NewOrderComponent);
    component = fixture.componentInstance;
    httpMock = TestBed.inject(HttpTestingController);
    router = TestBed.inject(Router);
    vi.spyOn(router, 'navigate').mockResolvedValue(true);
    fixture.detectChanges();
  });

  afterEach(() => {
    httpMock.verify();
  });

  it('arranca con el formulario inválido', () => {
    expect(component.form.invalid).toBe(true);
  });

  it('exige el nombre del cliente', () => {
    component.form.controls.customerName.setValue('');

    expect(component.form.controls.customerName.hasError('required')).toBe(true);

    component.form.controls.customerName.setValue('Ana Ruiz');

    expect(component.form.controls.customerName.valid).toBe(true);
  });

  it('rechaza un nombre de cliente solo con espacios', () => {
    component.form.controls.customerName.setValue('   ');

    expect(component.form.controls.customerName.hasError('required')).toBe(true);
  });

  it('admite hasta 120 caracteres en el nombre del cliente', () => {
    component.form.controls.customerName.setValue('a'.repeat(120));

    expect(component.form.controls.customerName.valid).toBe(true);

    component.form.controls.customerName.setValue('a'.repeat(121));
    component.form.controls.customerName.markAsTouched();

    expect(component.form.controls.customerName.hasError('maxlength')).toBe(true);
    expect(component.fieldError('customerName')).toBe('Máximo 120 caracteres.');
  });

  it('exige la placa del vehículo y limita a 16 caracteres', () => {
    component.form.controls.vehiclePlate.setValue('');

    expect(component.form.controls.vehiclePlate.hasError('required')).toBe(true);

    component.form.controls.vehiclePlate.setValue('ABC123');

    expect(component.form.controls.vehiclePlate.valid).toBe(true);

    component.form.controls.vehiclePlate.setValue('A'.repeat(17));

    expect(component.form.controls.vehiclePlate.hasError('maxlength')).toBe(true);
  });

  it('exige la descripción del vehículo y limita a 2000 caracteres', () => {
    component.form.controls.vehicleDescription.setValue('');

    expect(component.form.controls.vehicleDescription.hasError('required')).toBe(true);

    component.form.controls.vehicleDescription.setValue('a'.repeat(2000));

    expect(component.form.controls.vehicleDescription.valid).toBe(true);

    component.form.controls.vehicleDescription.setValue('a'.repeat(2001));

    expect(component.form.controls.vehicleDescription.hasError('maxlength')).toBe(true);
  });

  it('deja el código de aseguradora opcional pero limitado a 32 caracteres', () => {
    component.form.controls.insurerCode.setValue('');

    expect(component.form.controls.insurerCode.valid).toBe(true);

    component.form.controls.insurerCode.setValue('a'.repeat(32));

    expect(component.form.controls.insurerCode.valid).toBe(true);

    component.form.controls.insurerCode.setValue('a'.repeat(33));

    expect(component.form.controls.insurerCode.hasError('maxlength')).toBe(true);
  });

  it('no llama al backend cuando el formulario está inválido', () => {
    component.submit();

    httpMock.expectNone('/api/orders');
    expect(component.errorMessage()).toContain('Revisá los campos marcados');
  });

  it('envía la orden y vuelve al listado', () => {
    fillValidOrder();
    component.submit();

    const request = httpMock.expectOne('/api/orders');
    expect(request.request.method).toBe('POST');
    expect(request.request.body).toEqual({
      customerName: 'Ana Ruiz',
      vehiclePlate: 'ABC123',
      vehicleDescription: 'Toyota Corolla 2018',
      insurerCode: null,
    });

    request.flush(
      { orderId: '3f9d1c2e', status: 'RECIBIDA', createdAt: '2026-11-11T15:04:05.123Z' },
      { status: 201, statusText: 'Created' },
    );

    expect(component.submitting()).toBe(false);
    expect(router.navigate).toHaveBeenCalledWith(['/']);
  });

  it('envía el código de aseguradora cuando viene informado', () => {
    fillValidOrder();
    component.form.controls.insurerCode.setValue('SUR-001');
    component.submit();

    const request = httpMock.expectOne('/api/orders');
    expect(request.request.body.insurerCode).toBe('SUR-001');

    request.flush(
      { orderId: '3f9d1c2e', status: 'RECIBIDA', createdAt: '2026-11-11T15:04:05.123Z' },
      { status: 201, statusText: 'Created' },
    );
  });

  it('muestra el error cuando el backend rechaza la orden', () => {
    fillValidOrder();
    component.submit();

    httpMock
      .expectOne('/api/orders')
      .flush(
        { title: 'Bad Request', detail: 'La placa ya está registrada' },
        { status: 400, statusText: 'Bad Request' },
      );
    fixture.detectChanges();

    expect(component.submitting()).toBe(false);
    expect(component.errorMessage()).toBe('La placa ya está registrada');
    expect(fixture.nativeElement.querySelector('[role="alert"]')).toBeTruthy();
    expect(router.navigate).not.toHaveBeenCalled();
  });
});
