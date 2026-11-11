/** Estados y contratos de la API de órdenes expuesta por el gateway en `/api/orders`. */

export interface Order {
  orderId: string;
  customerName: string;
  vehiclePlate: string;
  vehicleDescription: string;
  insurerCode: string | null;
  status: string;
  createdAt: string;
}

export interface OrderPage {
  content: Order[];
  page: number;
  size: number;
  totalElements: number;
}

export interface OrderCreated {
  orderId: string;
  status: string;
  createdAt: string;
}

export interface OrderStatusHistoryEntry {
  eventId: string;
  fromStatus: string | null;
  toStatus: string;
  reason: string | null;
  changedBy: string;
  changedAt: string;
  eventHash: string;
  previousHash: string;
}

export interface OrderDetail extends Order {
  history: OrderStatusHistoryEntry[];
}

export interface CreateOrderRequest {
  customerName: string;
  vehiclePlate: string;
  vehicleDescription: string;
  insurerCode: string | null;
}
