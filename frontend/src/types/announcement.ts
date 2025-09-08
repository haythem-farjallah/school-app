export interface Announcement {
  id: number;
  title: string;
  body: string;
  startDate?: string;
  endDate?: string;
  isPublic: boolean;
  importance: 'LOW' | 'MEDIUM' | 'HIGH' | 'URGENT';
  createdAt: string;
  createdById?: number;
  createdByName?: string;
  publisherIds?: number[];
  targetType?: string;
  targetClassIds?: number[];
  targetClassNames?: string[];
}

export interface CreateAnnouncementRequest {
  title: string;
  body: string;
  startDate?: string;
  endDate?: string;
  isPublic: boolean;
  importance: 'LOW' | 'MEDIUM' | 'HIGH' | 'URGENT';
  publisherIds?: number[];
  // New targeting fields
  targetType?: 'CLASSES' | 'ALL_STAFF' | 'ALL_TEACHERS' | 'ALL_STUDENTS' | 'WHOLE_SCHOOL' | 'SPECIFIC_USERS';
  targetClassIds?: number[];
  targetUserIds?: number[];
  sendNotifications?: boolean;
}

export interface TeacherClass {
  id: number;
  name: string;
  gradeLevel: string;
  section: string;
  course: string;
}

export interface AnnouncementFilters {
  importance?: string;
  isPublic?: boolean;
  page?: number;
  size?: number;
}
