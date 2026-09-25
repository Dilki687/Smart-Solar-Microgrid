export interface BookingSlot {
  id?: string;
  slotId: string;
  stationId: string;
  startTime: string;
  endTime: string;
  totalCapacity: number;
  availableCapacity: number;
  isActive: boolean;
  createdAt?: string;
  updatedAt?: string;
}

export interface BookingSlotsResponse {
  slots: BookingSlot[];
}

export interface CreateBookingSlotRequest {
  stationId: string;
  startTime: string;
  endTime: string;
  totalCapacity: number;
}

export interface UpdateBookingSlotRequest {
  startTime: string;
  endTime: string;
  totalCapacity: number;
}