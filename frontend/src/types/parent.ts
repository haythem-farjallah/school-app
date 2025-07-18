export type ContactMethod = 'EMAIL' | 'PHONE' | 'SMS' | 'WHATSAPP' | 'IN_PERSON';

export interface Student {
  id: number;
  firstName: string;
  lastName: string;
  email: string;
  telephone?: string | null;
  birthday?: string | null;
  gender?: string | null;
  address?: string | null;
  gradeLevel?: string | null;
  enrollmentYear?: number | null;
}

export interface Parent {
  id: number;
  firstName: string;
  lastName: string;
  email: string;
  telephone?: string | null;
  birthday?: string | null;
  gender?: string | null;
  address?: string | null;
  preferredContactMethod?: ContactMethod | null;
  relation?: string | null;
  children?: Student[];
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
  preferredContactMethod?: ContactMethod | null;
  relation?: string | null;
  childrenEmails?: string[];
}

export interface UpdateParentData {
  telephone?: string | null;
  address?: string | null;
  preferredContactMethod?: ContactMethod | null;
  relation?: string | null;
  children?: string[];
} 