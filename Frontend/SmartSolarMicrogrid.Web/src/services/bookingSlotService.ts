import api from "./api";
import type {
  BookingSlot,
  BookingSlotsResponse,
  CreateBookingSlotRequest,
  UpdateBookingSlotRequest,
} from "../types/bookingSlot";

export const getBookingSlots = async (
  stationId?: string,
): Promise<BookingSlot[]> => {
  const response = await api.get<BookingSlotsResponse | BookingSlot[]>(
    "/api/bookings/slots",
    {
      params: stationId ? { stationId } : undefined,
    },
  );

  if (Array.isArray(response.data)) {
    return response.data;
  }

  return response.data.slots ?? [];
};

export const createBookingSlot = async (
  request: CreateBookingSlotRequest,
): Promise<void> => {
  await api.post("/api/bookings/slots", request);
};

export const updateBookingSlot = async (
  slotId: string,
  request: UpdateBookingSlotRequest,
): Promise<void> => {
  await api.put(`/api/bookings/slots/${slotId}`, request);
};

export const deactivateBookingSlot = async (
  slotId: string,
): Promise<void> => {
  await api.patch(`/api/bookings/slots/${slotId}/deactivate`);
};