import api from "./api";
import type {
  Reservation,
  ReservationsResponse,
  RejectReservationRequest,
  CreateReservationRequest,
  UpdateReservationRequest,
} from "../types/reservation";

export const getReservations = async (
  status?: string,
): Promise<Reservation[]> => {
  const response = await api.get<ReservationsResponse>(
    "/api/bookings/reservations",
    {
      params: status ? { status } : undefined,
    },
  );

  return response.data.reservations ?? [];
};

export const approveReservation = async (
  reservationId: string,
): Promise<void> => {
  await api.post(
    `/api/bookings/reservations/${reservationId}/approve`,
  );
};

export const rejectReservation = async (
  reservationId: string,
  request: RejectReservationRequest,
): Promise<void> => {
  await api.post(
    `/api/bookings/reservations/${reservationId}/reject`,
    request,
  );
};

export const approveReservationChange = async (
  reservationId: string,
): Promise<void> => {
  await api.post(
    `/api/bookings/reservations/${reservationId}/approve-change`,
  );
};

export const rejectReservationChange = async (
  reservationId: string,
  request: RejectReservationRequest,
): Promise<void> => {
  await api.post(
    `/api/bookings/reservations/${reservationId}/reject-change`,
    request,
  );
};
export const createReservation = async (
  request: CreateReservationRequest,
): Promise<void> => {
  await api.post("/api/bookings/reservations", request);
};
export const getMyReservations = async (): Promise<Reservation[]> => {
  const response = await api.get<ReservationsResponse>(
    "/api/bookings/reservations/my",
  );

  return response.data.reservations ?? [];
};
export const cancelReservation = async (
  reservationId: string,
  reason: string,
): Promise<void> => {
  await api.post(
    `/api/bookings/reservations/${reservationId}/cancel`,
    {
      reason,
    },
  );
};
export const requestReservationUpdate = async (
  reservationId: string,
  request: UpdateReservationRequest,
): Promise<void> => {
  await api.post(
    `/api/bookings/reservations/${reservationId}/request-update`,
    request,
  );
};