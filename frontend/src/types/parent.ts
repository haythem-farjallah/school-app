export interface Parent {
  id: number;
  firstName: string;
  lastName: string;
  email: string;
  telephone?: string | null;
  birthday?: string | null;
  gender?: string | null;
  address?: string | null;
  preferredContactMethod?: string | null;
}

export interface CreateParentData {
  profile: {
    firstName: string;
    lastName: string;
    email: string;
    telephone?: string | null;
    birthday?: string | null;
    gender?: string | null;
    address?: string | null;
  };
  preferredContactMethod?: string | null;
} 