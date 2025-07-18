import { z } from 'zod';

export const gradeFormSchema = z.object({
  enrollmentId: z.number().min(1, 'Enrollment is required'),
  courseId: z.number().min(1, 'Course is required'),
  score: z.number().min(0, 'Score must be 0 or greater'),
  maxScore: z.number().min(1, 'Maximum score must be at least 1'),
  weight: z.number().min(0.1, 'Weight must be at least 0.1').max(10, 'Weight cannot exceed 10'),
  content: z.string().min(1, 'Grade content is required').max(200, 'Content must be less than 200 characters'),
  description: z.string().max(500, 'Description must be less than 500 characters').optional(),
}).refine((data) => data.score <= data.maxScore, {
  message: 'Score cannot exceed maximum score',
  path: ['score'],
});

export type GradeFormData = z.infer<typeof gradeFormSchema>;

export const gradeFormFields = [
  {
    name: 'enrollmentId',
    label: 'Student Enrollment',
    type: 'select',
    placeholder: 'Select student enrollment',
    required: true,
    options: [], // Will be populated dynamically
  },
  {
    name: 'courseId',
    label: 'Course',
    type: 'select',
    placeholder: 'Select course',
    required: true,
    options: [], // Will be populated dynamically
  },
  {
    name: 'score',
    label: 'Score',
    type: 'number',
    placeholder: 'Enter score',
    required: true,
    min: 0,
    step: 0.01,
  },
  {
    name: 'maxScore',
    label: 'Maximum Score',
    type: 'number',
    placeholder: 'Enter maximum possible score',
    required: true,
    min: 1,
    step: 0.01,
  },
  {
    name: 'weight',
    label: 'Weight',
    type: 'number',
    placeholder: 'Enter grade weight (e.g., 1.0 for normal, 2.0 for double weight)',
    required: true,
    min: 0.1,
    max: 10,
    step: 0.1,
  },
  {
    name: 'content',
    label: 'Grade Content',
    type: 'text',
    placeholder: 'Enter grade content (e.g., "Midterm Exam", "Final Project")',
    required: true,
  },
  {
    name: 'description',
    label: 'Description',
    type: 'textarea',
    placeholder: 'Optional description or notes about this grade',
    required: false,
  },
];

// Helper function to calculate percentage
export const calculatePercentage = (score: number, maxScore: number): number => {
  return maxScore > 0 ? (score / maxScore) * 100 : 0;
};

// Helper function to get grade level
export const getGradeLevel = (percentage: number): string => {
  if (percentage >= 90) return 'A';
  if (percentage >= 80) return 'B';
  if (percentage >= 70) return 'C';
  if (percentage >= 60) return 'D';
  return 'F';
};

// Helper function to get grade color
export const getGradeColor = (percentage: number): string => {
  if (percentage >= 90) return 'text-green-600';
  if (percentage >= 80) return 'text-blue-600';
  if (percentage >= 70) return 'text-yellow-600';
  if (percentage >= 60) return 'text-orange-600';
  return 'text-red-600';
}; 