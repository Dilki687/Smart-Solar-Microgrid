export type BookingStatus =
  | "PENDING"
  | "CONFIRMED"
  | "COMPLETED"
  | "CANCELLED";

export interface Reservation {
  id?: string;
  reservationId: string;
  slotId: string;
  stationId: string;
  prosumerUserId: string;
  scheduledStartTime: string;
  scheduledEndTime: string;
  energyAmountKwh: number;
  status: BookingStatus | number;
  hasPendingChange: boolean;
changeRequestStatus?: "PENDING" | "APPROVED" | "REJECTED" | null;
  pendingSlotId?: string | null;
  pendingStationId?: string | null;
  pendingScheduledStartTime?: string | null;
  pendingScheduledEndTime?: string | null;
  pendingEnergyAmountKwh?: number | null;

  cancellationReason?: string | null;
  createdAt?: string;
  updatedAt?: string;
}

export interface ReservationsResponse {
  reservations: Reservation[];
}

export interface RejectReservationRequest {
  reason: string;
}

export interface RejectReservationChangeRequest {
  reason: string;
}
export interface CreateReservationRequest {
  slotId: string;
  stationId: string;
  scheduledStartTime: string;
  scheduledEndTime: string;
  energyAmountKwh: number;
}
export interface UpdateReservationRequest {
  slotId: string;
  scheduledStartTime: string;
  scheduledEndTime: string;
  energyAmountKwh: number;
}