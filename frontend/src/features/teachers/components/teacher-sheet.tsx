import * as React from "react"
import { Plus, User, Edit } from "lucide-react"
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
import type { Teacher, CreateTeacherData } from "@/types/teacher"
import { teacherSchema, teacherFields, type TeacherValues } from "../teacherForm.definition"

interface AddTeacherSheetProps {
  onSuccess?: () => void
}

export function AddTeacherSheet({ onSuccess }: AddTeacherSheetProps) {
  const [open, setOpen] = React.useState(false)

  const addTeacherMutation = useMutationApi<Teacher, CreateTeacherData>(
    async (data) => {
      const response = await http.post<Teacher>("/admin/teachers", data)
      return response.data
    },
    {
      onSuccess: () => {
        toast.success("Teacher added successfully!")
        setOpen(false)
        onSuccess?.()
      },
      onError: (error: unknown) => {
        const message = error && typeof error === 'object' && 'response' in error && 
          error.response && typeof error.response === 'object' && 'data' in error.response &&
          error.response.data && typeof error.response.data === 'object' && 'message' in error.response.data
          ? String(error.response.data.message) : "Failed to add teacher"
        toast.error(message)
      },
    }
  )

  const addTeacherRecipe: FormRecipe = {
    schema: teacherSchema,
    fields: teacherFields,
    onSubmit: async (values: unknown) => {
      const formValues = values as TeacherValues;
      const teacherData: CreateTeacherData = {
        profile: {
          firstName: formValues.firstName,
          lastName: formValues.lastName,
          email: formValues.email,
          telephone: formValues.telephone,
          birthday: formValues.birthday,
          gender: formValues.gender,
          address: formValues.address,
        },
        qualifications: formValues.qualifications,
        subjectsTaught: formValues.subjectsTaught,
        availableHours: formValues.availableHours,
        schedulePreferences: formValues.schedulePreferences,
      };
      await addTeacherMutation.mutateAsync(teacherData);
    },
  }

  return (
    <Sheet open={open} onOpenChange={setOpen}>
      <SheetTrigger asChild>
        <Button className="bg-gradient-to-r from-blue-600 to-indigo-600 hover:from-blue-700 hover:to-indigo-700 shadow-lg hover:shadow-xl transition-all duration-300 transform hover:scale-105">
          <Plus className="mr-2 h-4 w-4" />
          Add Teacher
        </Button>
      </SheetTrigger>
      <SheetContent className="w-[400px] sm:w-[540px] bg-white border-l-4 border-blue-500 overflow-y-auto max-h-screen">
        <SheetHeader className="space-y-4 pb-6 border-b border-gray-100">
          <div className="flex items-center space-x-3">
            <div className="p-2 bg-gradient-to-br from-blue-100 to-indigo-100 rounded-lg">
              <User className="h-6 w-6 text-blue-600" />
            </div>
            <div>
              <SheetTitle className="text-2xl font-bold bg-gradient-to-r from-blue-900 to-indigo-700 bg-clip-text text-transparent">
                Add New Teacher
              </SheetTitle>
              <SheetDescription className="text-gray-600">
                Create a new teacher profile with all the necessary details
              </SheetDescription>
            </div>
          </div>
        </SheetHeader>

        <div className="py-6">
          <AutoForm
            recipe={addTeacherRecipe}
            defaultValues={{
              firstName: "",
              lastName: "",
              email: "",
              telephone: "",
              birthday: "",
              gender: "",
              address: "",
              qualifications: "",
              subjectsTaught: "",
              availableHours: 20,
              schedulePreferences: "Morning",
            }}
            submitLabel="Add Teacher"
            submitClassName="bg-gradient-to-r from-blue-600 to-indigo-600 hover:from-blue-700 hover:to-indigo-700 shadow-lg hover:shadow-xl transition-all duration-300"
          />
        </div>
      </SheetContent>
    </Sheet>
  )
}

interface EditTeacherSheetProps {
  teacher: Teacher
  trigger?: React.ReactNode
  onSuccess?: () => void
}

export function EditTeacherSheet({ teacher, trigger, onSuccess }: EditTeacherSheetProps) {
  const [open, setOpen] = React.useState(false)

  const editTeacherMutation = useMutationApi<Teacher, TeacherValues>(
    async (data) => {
      const response = await http.patch<Teacher>(`/admin/teachers/${teacher.id}`, data)
      return response.data
    },
    {
      onSuccess: () => {
        toast.success("Teacher updated successfully!")
        setOpen(false)
        onSuccess?.()
      },
      onError: (error: unknown) => {
        const message = error && typeof error === 'object' && 'response' in error && 
          error.response && typeof error.response === 'object' && 'data' in error.response &&
          error.response.data && typeof error.response.data === 'object' && 'message' in error.response.data
          ? String(error.response.data.message) : "Failed to update teacher"
        toast.error(message)
      },
    }
  )

  const editTeacherRecipe: FormRecipe = {
    schema: teacherSchema,
    fields: teacherFields,
    onSubmit: async (values: unknown) => {
      await editTeacherMutation.mutateAsync(values as TeacherValues)
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
      <SheetContent className="w-[400px] sm:w-[540px] bg-white border-l-4 border-green-500 overflow-y-auto max-h-screen">
        <SheetHeader className="space-y-4 pb-6 border-b border-gray-100">
          <div className="flex items-center space-x-3">
            <div className="p-2 bg-gradient-to-br from-green-100 to-emerald-100 rounded-lg">
              <Edit className="h-6 w-6 text-green-600" />
            </div>
            <div>
              <SheetTitle className="text-2xl font-bold bg-gradient-to-r from-green-900 to-emerald-700 bg-clip-text text-transparent">
                Edit Teacher
              </SheetTitle>
              <SheetDescription className="text-gray-600">
                Update teacher information and settings
              </SheetDescription>
            </div>
          </div>
        </SheetHeader>

        <div className="py-6">
          <AutoForm
            recipe={editTeacherRecipe}
            defaultValues={{
              firstName: teacher.firstName,
              lastName: teacher.lastName,
              email: teacher.email,
              telephone: teacher.telephone || "",
              birthday: teacher.birthday || "",
              gender: teacher.gender || "",
              address: teacher.address || "",
              qualifications: teacher.qualifications,
              subjectsTaught: teacher.subjectsTaught,
              availableHours: teacher.availableHours,
              schedulePreferences: teacher.schedulePreferences,
            }}
            submitLabel="Update Teacher"
            submitClassName="bg-gradient-to-r from-green-600 to-emerald-600 hover:from-green-700 hover:to-emerald-700 shadow-lg hover:shadow-xl transition-all duration-300"
          />
        </div>
      </SheetContent>
    </Sheet>
  )
} 