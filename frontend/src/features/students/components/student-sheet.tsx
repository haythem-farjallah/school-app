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
import type { Student, CreateStudentData } from "@/types/student"
import { studentSchema, studentFields, type StudentValues } from "../studentForm.definition"
import { useUpdateStudent } from "../hooks/use-students"

interface AddStudentSheetProps {
  onSuccess?: () => void
}

export function AddStudentSheet({ onSuccess }: AddStudentSheetProps) {
  const [open, setOpen] = React.useState(false)

  const addStudentMutation = useMutationApi<Student, CreateStudentData>(
    async (data) => {
      console.log("➕ AddStudentSheet - Creating student:", data);
      const response = await http.post<{ status: string; data: Student }>("/v1/students", data)
      console.log("➕ AddStudentSheet - Response:", response.data);
      return response.data.data
    },
    {
      onSuccess: (data) => {
        console.log("✅ AddStudentSheet - Student created successfully:", data);
        toast.success("Student added successfully!")
        setOpen(false)
        onSuccess?.()
      },
      onError: (error: unknown) => {
        console.error("❌ AddStudentSheet - Failed to create student:", error);
        const message = error && typeof error === 'object' && 'response' in error && 
          error.response && typeof error.response === 'object' && 'data' in error.response &&
          error.response.data && typeof error.response.data === 'object' && 'message' in error.response.data
          ? String(error.response.data.message) : "Failed to add student"
        toast.error(message)
      },
    }
  )

  const addStudentRecipe: FormRecipe = {
    schema: studentSchema,
    fields: studentFields,
    onSubmit: async (values: unknown) => {
      console.log("➕ AddStudentSheet - Form submitted with values:", values);
      const formValues = values as StudentValues;
      const studentData: CreateStudentData = {
        profile: {
          firstName: formValues.firstName,
          lastName: formValues.lastName,
          email: formValues.email,
          telephone: formValues.telephone,
          birthday: formValues.birthday,
          gender: formValues.gender,
          address: formValues.address,
        },
        gradeLevel: formValues.gradeLevel.trim().toUpperCase(),
        enrollmentYear: formValues.enrollmentYear || new Date().getFullYear(),
      };
      console.log("➕ AddStudentSheet - Transformed student data:", studentData);
      await addStudentMutation.mutateAsync(studentData);
    },
  }

  return (
    <Sheet open={open} onOpenChange={setOpen}>
      <SheetTrigger asChild>
        <Button className="bg-gradient-to-r from-blue-600 to-purple-600 hover:from-blue-700 hover:to-purple-700 shadow-lg hover:shadow-xl transition-all duration-300">
          <Plus className="mr-2 h-4 w-4" />
          Add Student
        </Button>
      </SheetTrigger>
      <SheetContent className="w-[400px] sm:w-[540px] bg-gradient-to-br from-white to-blue-50 border-l-4 border-blue-500">
        <SheetHeader className="pb-6 border-b border-blue-100">
          <div className="flex items-center gap-3">
            <div className="p-2 bg-gradient-to-br from-blue-500 to-purple-600 rounded-lg">
              <User className="h-5 w-5 text-white" />
            </div>
            <div>
              <SheetTitle className="text-xl font-bold bg-gradient-to-r from-blue-800 to-purple-800 bg-clip-text text-transparent">
                Add New Student
              </SheetTitle>
              <SheetDescription className="text-gray-600">
                Fill in the student information below to add them to the system.
              </SheetDescription>
            </div>
          </div>
        </SheetHeader>
        <div className="py-6">
          <AutoForm
            recipe={addStudentRecipe}
            submitLabel="Add Student"
            submitClassName="bg-gradient-to-r from-blue-600 to-purple-600 hover:from-blue-700 hover:to-purple-700 shadow-lg hover:shadow-xl transition-all duration-300"
          />
        </div>
      </SheetContent>
    </Sheet>
  )
}

interface EditStudentSheetProps {
  student: Student
  trigger?: React.ReactNode
  onSuccess?: () => void
}

export function EditStudentSheet({ student, trigger, onSuccess }: EditStudentSheetProps) {
  const [open, setOpen] = React.useState(false)

  const editStudentMutation = useUpdateStudent()

  const editStudentRecipe: FormRecipe = {
    schema: studentSchema,
    fields: studentFields,
    onSubmit: async (values: unknown) => {
      console.log("✏️ EditStudentSheet - Form submitted with values:", values);
      const formValues = values as StudentValues;
      const studentData: CreateStudentData & { id: number } = {
        id: student.id,
        profile: {
          firstName: formValues.firstName,
          lastName: formValues.lastName,
          email: formValues.email,
          telephone: formValues.telephone,
          birthday: formValues.birthday,
          gender: formValues.gender,
          address: formValues.address,
        },
        gradeLevel: formValues.gradeLevel.trim().toUpperCase(),
        enrollmentYear: formValues.enrollmentYear || new Date().getFullYear(),
      };
      console.log("✏️ EditStudentSheet - Transformed data:", studentData)
      await editStudentMutation.mutateAsync(studentData)
    },
  }

  React.useEffect(() => {
    if (editStudentMutation.isSuccess) {
      console.log("✅ EditStudentSheet - Student updated successfully");
      toast.success("Student updated successfully!")
      setOpen(false)
      onSuccess?.()
    }
  }, [editStudentMutation.isSuccess, onSuccess])

  React.useEffect(() => {
    if (editStudentMutation.isError) {
      console.error("❌ EditStudentSheet - Update failed:", editStudentMutation.error)
      const message = editStudentMutation.error?.response?.data && 
        typeof editStudentMutation.error.response.data === 'object' && 
        'message' in editStudentMutation.error.response.data
        ? String(editStudentMutation.error.response.data.message)
        : "Failed to update student"
      toast.error(message)
    }
  }, [editStudentMutation.isError, editStudentMutation.error])

  return (
    <Sheet open={open} onOpenChange={setOpen}>
      <SheetTrigger asChild>
        {trigger || (
          <Button variant="ghost" size="sm" className="hover:bg-blue-50 hover:text-blue-700 transition-colors duration-200">
            <Edit className="h-4 w-4" />
          </Button>
        )}
      </SheetTrigger>
      <SheetContent className="w-[400px] sm:w-[540px] bg-gradient-to-br from-white to-green-50 border-l-4 border-green-500 overflow-y-auto max-h-screen">
        <SheetHeader className="space-y-4 pb-6 border-b border-gray-100">
          <div className="flex items-center space-x-3">
            <div className="p-2 bg-gradient-to-br from-green-100 to-emerald-100 rounded-lg">
              <Edit className="h-6 w-6 text-green-600" />
            </div>
            <div>
              <SheetTitle className="text-2xl font-bold bg-gradient-to-r from-green-900 to-emerald-700 bg-clip-text text-transparent">
                Edit Student
              </SheetTitle>
              <SheetDescription className="text-gray-600">
                Update {student?.firstName || 'Student'} {student?.lastName || ''}'s information
              </SheetDescription>
            </div>
          </div>
        </SheetHeader>

        <div className="py-6">
          <AutoForm
            recipe={editStudentRecipe}
            defaultValues={{
              firstName: student?.firstName || "",
              lastName: student?.lastName || "",
              email: student?.email || "",
              telephone: student?.telephone || "",
              birthday: student?.birthday || "",
              gender: student?.gender || "",
              address: student?.address || "",
              gradeLevel: student?.gradeLevel || "",
              enrollmentYear: student?.enrollmentYear || new Date().getFullYear(),
            }}
            submitLabel="Update Student"
            submitClassName="bg-gradient-to-r from-green-600 to-emerald-600 hover:from-green-700 hover:to-emerald-700 shadow-lg hover:shadow-xl transition-all duration-300"
          />
        </div>
      </SheetContent>
    </Sheet>
  )
} 