export interface Staff {
  id: number;
  firstName: string;
  lastName: string;
  email: string;
  telephone?: string | null;
  birthday?: string | null;
  gender?: string | null;
  address?: string | null;
  staffType: StaffType;
  department: string;
  createdAt: string;
  updatedAt: string;
}

export enum StaffType {
  ADMINISTRATIVE = 'ADMINISTRATIVE',
  MAINTENANCE = 'MAINTENANCE',
  SECURITY = 'SECURITY',
  LIBRARIAN = 'LIBRARIAN',
  COUNSELOR = 'COUNSELOR',
}

export interface CreateStaffRequest {
  profile: {
    firstName: string;
    lastName: string;
    email: string;
    telephone: string;
    birthday?: string;
    gender: 'M' | 'F' | 'O';
    address: string;
    role: 'STAFF';
  };
  staffType: StaffType;
  department: string;
}

export interface UpdateStaffRequest {
  profile?: {
    firstName?: string;
    lastName?: string;
    email?: string;
    telephone?: string;
    birthday?: string;
    gender?: 'M' | 'F' | 'O';
    address?: string;
  };
  staffType?: StaffType;
  department?: string;
}

export interface StaffFilters {
  staffType?: StaffType;
  department?: string;
  search?: string;
}

export interface StaffResponse {
  content: Staff[];
  totalElements: number;
  totalPages: number;
  number: number;
  size: number;
} 