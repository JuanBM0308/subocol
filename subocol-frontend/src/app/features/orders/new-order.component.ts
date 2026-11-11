import { HttpErrorResponse } from '@angular/common/http';
import { Component, inject, signal } from '@angular/core';
import {
  AbstractControl,
  NonNullableFormBuilder,
  ReactiveFormsModule,
  ValidationErrors,
  Validators,
} from '@angular/forms';
import { Router, RouterLink } from '@angular/router';

import { describeApiError } from '../../core/api-error';
import { CreateOrderRequest } from '../../core/order.models';
import { OrderService } from '../../core/order.service';

/** Rechaza valores que solo contienen espacios: no es lo mismo que estar vacío. */
function nonBlankValidator(control: AbstractControl): ValidationErrors | null {
  const value = control.value;
  if (typeof value !== 'string') {
    return null;
  }
  return value.trim().length > 0 ? null : { required: true };
}

/**
 * Alta de una orden de servicio.
 *
 * Los límites de longitud replican los del backend (`docs/mvp-contract.md`):
 * el cliente avisa antes de gastar un viaje al gateway, y el servidor sigue
 * siendo la autoridad de la validación.
 */
@Component({
  selector: 'app-new-order',
  imports: [ReactiveFormsModule, RouterLink],
  templateUrl: './new-order.component.html',
})
export class NewOrderComponent {
  private readonly formBuilder = inject(NonNullableFormBuilder);
  private readonly orderService = inject(OrderService);
  private readonly router = inject(Router);

  /** `true` mientras la orden se está guardando. */
  public readonly submitting = signal(false);
  /** Mensaje de error visible; `null` cuando no hay error. */
  public readonly errorMessage = signal<string | null>(null);

  public readonly form = this.formBuilder.group({
    customerName: this.formBuilder.control('', [
      Validators.required,
      nonBlankValidator,
      Validators.maxLength(120),
    ]),
    vehiclePlate: this.formBuilder.control('', [
      Validators.required,
      nonBlankValidator,
      Validators.maxLength(16),
    ]),
    vehicleDescription: this.formBuilder.control('', [
      Validators.required,
      nonBlankValidator,
      Validators.maxLength(2000),
    ]),
    insurerCode: this.formBuilder.control('', [Validators.maxLength(32)]),
  });

  /** Envía la orden. Si el backend responde, vuelve al listado. */
  public submit(): void {
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      this.errorMessage.set('Revisá los campos marcados: hay datos obligatorios sin completar.');
      return;
    }

    const value = this.form.getRawValue();
    const request: CreateOrderRequest = {
      customerName: value.customerName.trim(),
      vehiclePlate: value.vehiclePlate.trim(),
      vehicleDescription: value.vehicleDescription.trim(),
      insurerCode: value.insurerCode.trim().length > 0 ? value.insurerCode.trim() : null,
    };

    this.submitting.set(true);
    this.errorMessage.set(null);

    this.orderService.createOrder(request).subscribe({
      next: () => {
        this.submitting.set(false);
        void this.router.navigate(['/']);
      },
      error: (error: HttpErrorResponse) => {
        this.submitting.set(false);
        this.errorMessage.set(describeApiError(error));
      },
    });
  }

  /** Mensaje de validación de un campo, o `null` si el campo todavía está bien. */
  public fieldError(fieldName: string): string | null {
    const control = this.form.get(fieldName);
    if (!control || control.valid || !(control.touched || control.dirty)) {
      return null;
    }
    if (control.hasError('required')) {
      return 'Este campo es obligatorio.';
    }
    if (control.hasError('maxlength')) {
      const requiredLength = control.getError('maxlength')?.requiredLength as number;
      return `Máximo ${requiredLength} caracteres.`;
    }
    return 'Revisá el valor ingresado.';
  }
}
