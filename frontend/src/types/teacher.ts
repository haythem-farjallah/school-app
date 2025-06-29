export interface Teacher {
    id: number;
    firstName: string;
    lastName: string;
    email: string;
    telephone?: string | null;
    birthday?: string | null;
    gender?: string | null;
    address?: string | null;
    qualifications: string;
    subjectsTaught: string;
    availableHours: number;
    schedulePreferences: string;
  }
  
  export interface CreateTeacherData {
    profile: {
      firstName: string;
      lastName: string;
      email: string;
      telephone?: string | null;
      birthday?: string | null;
      gender?: string | null;
      address?: string | null;
    };
    qualifications: string;
    subjectsTaught: string;
    availableHours: number;
    schedulePreferences: string;
  }

