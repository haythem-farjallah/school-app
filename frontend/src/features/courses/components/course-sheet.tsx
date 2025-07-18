import * as React from "react"
import { Plus, BookOpen, Edit } from "lucide-react"
import toast from "react-hot-toast"

import { Button } from "@/components/ui/button"
import {
  Sheet,
  SheetContent,
  SheetDescription,
  SheetHeader,
  SheetTitle,
  SheetTrigger,
} from "@/components/ui/sheet"
import { AutoForm } from "@/form/AutoForm"
import type { FormRecipe } from "@/form/types"
import { useMutationApi } from "@/hooks/useMutationApi"
import { http } from "@/lib/http"
import type { Course } from "@/types/course"
import { courseSchema, courseFields, type CourseValues } from "../courseForm.definition"

interface AddCourseSheetProps {
  onSuccess?: () => void
}

export function AddCourseSheet({ onSuccess }: AddCourseSheetProps) {
  const [open, setOpen] = React.useState(false)

  const addCourseMutation = useMutationApi<Course, CourseValues>(
    async (data) => {
      const response = await http.post<Course>("/v1/courses", data)
      return response.data
    },
    {
      onSuccess: () => {
        toast.success("Course added successfully!")
        setOpen(false)
        onSuccess?.()
      },
      onError: (error: unknown) => {
        const message = error && typeof error === 'object' && 'response' in error && 
          error.response && typeof error.response === 'object' && 'data' in error.response &&
          error.response.data && typeof error.response.data === 'object' && 'message' in error.response.data
          ? String(error.response.data.message) : "Failed to add course"
        toast.error(message)
      },
    }
  )

  const addCourseRecipe: FormRecipe = {
    schema: courseSchema,
    fields: courseFields,
    onSubmit: async (values: unknown) => {
      console.log("📝 Course form values:", values);
      const typedValues = values as CourseValues;
      console.log("📝 Values types:", {
        name: typeof typedValues.name,
        color: typeof typedValues.color,
        credit: typeof typedValues.credit,
        weeklyCapacity: typeof typedValues.weeklyCapacity,
        teacherId: typeof typedValues.teacherId,
      });
      await addCourseMutation.mutateAsync(typedValues)
    },
  }

  return (
    <Sheet open={open} onOpenChange={setOpen}>
      <SheetTrigger asChild>
        <Button className="bg-gradient-to-r from-blue-600 to-indigo-600 hover:from-blue-700 hover:to-indigo-700 shadow-lg hover:shadow-xl transition-all duration-300 transform hover:scale-105">
          <Plus className="mr-2 h-4 w-4" />
          Add Course
        </Button>
      </SheetTrigger>
      <SheetContent className="w-[400px] sm:w-[540px] bg-white border-l-4 border-blue-500">
        <SheetHeader className="space-y-4 pb-6 border-b border-gray-100">
          <div className="flex items-center space-x-3">
            <div className="p-2 bg-gradient-to-br from-blue-100 to-indigo-100 rounded-lg">
              <BookOpen className="h-6 w-6 text-blue-600" />
            </div>
            <div>
              <SheetTitle className="text-2xl font-bold bg-gradient-to-r from-blue-900 to-indigo-700 bg-clip-text text-transparent">
                Add New Course
              </SheetTitle>
              <SheetDescription className="text-gray-600">
                Create a new course with all the necessary details
              </SheetDescription>
            </div>
          </div>
        </SheetHeader>

        <div className="py-6">
          <AutoForm
            recipe={addCourseRecipe}
            defaultValues={{
              name: "",
              color: "#3B82F6",
              credit: 1.0,
              weeklyCapacity: 3,
              teacherId: undefined,
            }}
            submitLabel="Add Course"
            submitClassName="bg-gradient-to-r from-blue-600 to-indigo-600 hover:from-blue-700 hover:to-indigo-700 shadow-lg hover:shadow-xl transition-all duration-300"
          />
        </div>
      </SheetContent>
    </Sheet>
  )
}

interface EditCourseSheetProps {
  course: Course
  trigger?: React.ReactNode
  onSuccess?: () => void
}

export function EditCourseSheet({ course, trigger, onSuccess }: EditCourseSheetProps) {
  const [open, setOpen] = React.useState(false)

  const editCourseMutation = useMutationApi<Course, CourseValues>(
    async (data) => {
      const response = await http.put<Course>(`/v1/courses/${course.id}`, data)
      return response.data
    },
    {
      onSuccess: () => {
        toast.success("Course updated successfully!")
        setOpen(false)
        onSuccess?.()
      },
      onError: (error: unknown) => {
        const message = error && typeof error === 'object' && 'response' in error && 
          error.response && typeof error.response === 'object' && 'data' in error.response &&
          error.response.data && typeof error.response.data === 'object' && 'message' in error.response.data
          ? String(error.response.data.message) : "Failed to update course"
        toast.error(message)
      },
    }
  )

  const editCourseRecipe: FormRecipe = {
    schema: courseSchema,
    fields: courseFields,
    onSubmit: async (values: unknown) => {
      console.log("📝 Edit course form values:", values);
      const typedValues = values as CourseValues;
      console.log("📝 Edit values types:", {
        name: typeof typedValues.name,
        color: typeof typedValues.color,
        credit: typeof typedValues.credit,
        weeklyCapacity: typeof typedValues.weeklyCapacity,
        teacherId: typeof typedValues.teacherId,
      });
      await editCourseMutation.mutateAsync(typedValues)
    },
  }

  return (
    <Sheet open={open} onOpenChange={setOpen}>
      <SheetTrigger asChild>
        {trigger || (
          <Button variant="outline" size="sm">
            <Edit className="mr-2 h-4 w-4" />
            Edit
          </Button>
        )}
      </SheetTrigger>
      <SheetContent className="w-[400px] sm:w-[540px] bg-white border-l-4 border-green-500">
        <SheetHeader className="space-y-4 pb-6 border-b border-gray-100">
          <div className="flex items-center space-x-3">
            <div className="p-2 bg-gradient-to-br from-green-100 to-emerald-100 rounded-lg">
              <Edit className="h-6 w-6 text-green-600" />
            </div>
            <div>
              <SheetTitle className="text-2xl font-bold bg-gradient-to-r from-green-900 to-emerald-700 bg-clip-text text-transparent">
                Edit Course
              </SheetTitle>
              <SheetDescription className="text-gray-600">
                Update course information and settings
              </SheetDescription>
            </div>
          </div>
        </SheetHeader>

        <div className="py-6">
          <AutoForm
            recipe={editCourseRecipe}
            defaultValues={{
              name: course.name,
              color: course.color,
              credit: course.credit,
              weeklyCapacity: course.weeklyCapacity,
              teacherId: course.teacherId,
            }}
            submitLabel="Update Course"
            submitClassName="bg-gradient-to-r from-green-600 to-emerald-600 hover:from-green-700 hover:to-emerald-700 shadow-lg hover:shadow-xl transition-all duration-300"
          />
        </div>
      </SheetContent>
    </Sheet>
  )
} 