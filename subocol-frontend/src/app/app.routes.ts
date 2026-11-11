import { Routes } from '@angular/router';

import { NewOrderComponent } from './features/orders/new-order.component';
import { OrderDetailComponent } from './features/orders/order-detail.component';
import { OrderListComponent } from './features/orders/order-list.component';

export const routes: Routes = [
  {
    path: '',
    component: OrderListComponent,
    title: 'Órdenes · Taller 360 Subocol',
  },
  {
    path: 'nueva',
    component: NewOrderComponent,
    title: 'Nueva orden · Taller 360 Subocol',
  },
  {
    path: 'detalle/:id',
    component: OrderDetailComponent,
    title: 'Detalle de la orden · Taller 360 Subocol',
  },
  { path: '**', redirectTo: '' },
];
