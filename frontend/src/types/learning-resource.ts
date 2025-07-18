export interface LearningResource {
  id: number;
  title: string;
  description: string;
  url?: string;
  filename?: string;
  type: ResourceType;
  thumbnailUrl?: string;
  duration?: number;
  isPublic: boolean;
  teacherIds: number[];
  classIds: number[];
  courseIds: number[];
  createdAt: string;
  updatedAt: string;
}

export enum ResourceType {
  VIDEO = 'VIDEO',
  DOCUMENT = 'DOCUMENT',
  PRESENTATION = 'PRESENTATION',
  AUDIO = 'AUDIO',
  IMAGE = 'IMAGE',
  LINK = 'LINK',
  OTHER = 'OTHER',
}

export interface CreateLearningResourceRequest {
  title: string;
  description: string;
  url?: string;
  type: ResourceType;
  thumbnailUrl?: string;
  duration?: number;
  isPublic: boolean;
  teacherIds?: number[];
  classIds?: number[];
  courseIds?: number[];
}

export interface UpdateLearningResourceRequest {
  title?: string;
  description?: string;
  url?: string;
  type?: ResourceType;
  thumbnailUrl?: string;
  duration?: number;
  isPublic?: boolean;
  teacherIds?: number[];
  classIds?: number[];
  courseIds?: number[];
}

export interface LearningResourceFilters {
  type?: ResourceType;
  teacherId?: number;
  classId?: number;
  courseId?: number;
  search?: string;
  isPublic?: boolean;
}

export interface LearningResourceResponse {
  content: LearningResource[];
  totalElements: number;
  totalPages: number;
  number: number;
  size: number;
}

export interface ResourceComment {
  id: number;
  content: string;
  resourceId: number;
  userId: number;
  userName: string;
  createdAt: string;
  updatedAt: string;
}

export interface CreateResourceCommentRequest {
  content: string;
  resourceId: number;
}

export interface ResourceCommentResponse {
  content: ResourceComment[];
  totalElements: number;
  totalPages: number;
  number: number;
  size: number;
}

export interface ResourceUploadRequest {
  file: File;
  title: string;
  description: string;
  type: ResourceType;
  thumbnailUrl?: string;
  duration?: number;
  isPublic: boolean;
  classIds?: number[];
  courseIds?: number[];
} 