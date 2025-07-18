export interface Room {
  id: number;
  name: string;
  capacity: number;
  roomType: RoomType;
}

export enum RoomType {
  CLASSROOM = 'CLASSROOM',
  LABORATORY = 'LABORATORY',
  AUDITORIUM = 'AUDITORIUM',
  OFFICE = 'OFFICE',
  GYM = 'GYM',
}

export interface CreateRoomRequest {
  name: string;
  capacity: number;
  roomType: RoomType;
}

export interface UpdateRoomRequest {
  name: string;
  capacity: number;
  roomType: RoomType;
}

export interface RoomFilters {
  roomType?: RoomType;
  minCapacity?: number;
  name?: string;
}

export interface RoomResponse {
  content: Room[];
  totalElements: number;
  totalPages: number;
  number: number;
  size: number;
}

export interface RoomAvailabilityRequest {
  roomId: number;
  startTime: string;
  endTime: string;
  date: string;
}

export interface RoomBooking {
  id: number;
  roomId: number;
  startTime: string;
  endTime: string;
  date: string;
  purpose: string;
  bookedBy: string;
  createdAt: string;
} 