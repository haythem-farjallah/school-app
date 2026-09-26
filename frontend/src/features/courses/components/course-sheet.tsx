import * as React from "react"
import { Edit } from "lucide-react"
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
import { getApiErrorMessage } from "@/lib/api-error"
import { http } from "@/lib/http"
import type { Course } from "@/types/course"
import { courseSchema, courseFields, type CourseValues } from "../courseForm.definition"

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
        const message = getApiErrorMessage(error, "Failed to update course")
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