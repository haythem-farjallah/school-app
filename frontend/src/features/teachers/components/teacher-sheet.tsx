import * as React from "react"
import { Plus, User, Edit, Mail, Phone } from "lucide-react"
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
import type { Teacher, CreateTeacherData, UpdateTeacherData } from "@/types/teacher"
import { teacherSchema, teacherFields, teacherUpdateSchema, teacherUpdateFields, type TeacherValues, type TeacherUpdateValues } from "../teacherForm.definition"
import { useUpdateTeacher } from "../hooks/use-teachers"

interface AddTeacherSheetProps {
  onSuccess?: () => void
}

export function AddTeacherSheet({ onSuccess }: AddTeacherSheetProps) {
  const [open, setOpen] = React.useState(false)

  const addTeacherMutation = useMutationApi<Teacher, CreateTeacherData>(
    async (data) => {
      const response = await http.post<{ status: string; data: Teacher }>("/admin/teachers", data)
      return (response as unknown as { status: string; data: Teacher }).data
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
        availableHours: typeof formValues.availableHours === 'string' 
          ? parseInt(formValues.availableHours, 10) 
          : formValues.availableHours,
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

  const editTeacherMutation = useUpdateTeacher()

  // Debug: Log the teacher data and form fields
  React.useEffect(() => {
    if (open) {
      console.log("🔍 EditTeacherSheet - Teacher data:", teacher);
      console.log("🔍 EditTeacherSheet - Update fields:", teacherUpdateFields);
      console.log("🔍 EditTeacherSheet - Default values:", {
        telephone: teacher.telephone || "",
        address: teacher.address || "",
        qualifications: teacher.qualifications,
        subjectsTaught: teacher.subjectsTaught,
        availableHours: teacher.availableHours,
        schedulePreferences: teacher.schedulePreferences,
      });
    }
  }, [open, teacher]);

  const editTeacherRecipe: FormRecipe = {
    schema: teacherUpdateSchema,
    fields: teacherUpdateFields,
    onSubmit: async (values: unknown) => {
      const formValues = values as TeacherUpdateValues;
      
      // Debug: Log the raw form values
      console.log("🔍 EditTeacherSheet - Raw form values:", formValues);
      console.log("🔍 EditTeacherSheet - Telephone value:", formValues.telephone, "Type:", typeof formValues.telephone);
      console.log("🔍 EditTeacherSheet - Address value:", formValues.address, "Type:", typeof formValues.address);
      
      // For updates, send the data in the format expected by TeacherUpdateDto (flat structure)
      const teacherData: UpdateTeacherData & { id: number } = {
        id: teacher.id,
        // Send fields directly (not nested in profile) as expected by TeacherUpdateDto
        telephone: formValues.telephone && formValues.telephone.trim() !== "" ? formValues.telephone : null,
        address: formValues.address && formValues.address.trim() !== "" ? formValues.address : null,
        qualifications: formValues.qualifications,
        subjectsTaught: formValues.subjectsTaught,
        availableHours: typeof formValues.availableHours === 'string' 
          ? parseInt(formValues.availableHours, 10) 
          : formValues.availableHours,
        schedulePreferences: formValues.schedulePreferences,
      };
      console.log("✏️ EditTeacherSheet - Transformed data:", teacherData)
      await editTeacherMutation.mutateAsync(teacherData)
    },
  }

  // Handle success and error states with useCallback to prevent infinite loops
  const handleSuccess = React.useCallback(() => {
    toast.success("Teacher updated successfully!")
    setOpen(false)
    // Call onSuccess callback if provided
    if (onSuccess) {
      onSuccess()
    }
    // Reset the mutation state to prevent infinite loops
    editTeacherMutation.reset()
  }, [onSuccess, editTeacherMutation])

  const handleError = React.useCallback(() => {
    console.error("❌ EditTeacherSheet - Update failed:", editTeacherMutation.error)
    const error = editTeacherMutation.error as { response?: { data?: { message?: string } } };
    const message = error?.response?.data?.message || "Failed to update teacher"
    toast.error(message)
    // Reset the mutation state to prevent infinite loops
    editTeacherMutation.reset()
  }, [editTeacherMutation])

  React.useEffect(() => {
    if (editTeacherMutation.isSuccess) {
      handleSuccess()
    }
  }, [editTeacherMutation.isSuccess, handleSuccess])

  React.useEffect(() => {
    if (editTeacherMutation.isError) {
      handleError()
    }
  }, [editTeacherMutation.isError, handleError])

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
          {/* Display non-editable fields as read-only information */}
          <div className="mb-6 p-4 bg-gray-50 rounded-lg border border-gray-200">
            <h3 className="text-sm font-medium text-gray-700 mb-3">Teacher Information (Read-only)</h3>
            <div className="grid grid-cols-1 gap-3">
              <div className="flex items-center space-x-2">
                <User className="h-4 w-4 text-gray-500" />
                <span className="text-sm text-gray-600">
                  <span className="font-medium">Name:</span> {teacher.firstName} {teacher.lastName}
                </span>
              </div>
              <div className="flex items-center space-x-2">
                <Mail className="h-4 w-4 text-gray-500" />
                <span className="text-sm text-gray-600">
                  <span className="font-medium">Email:</span> {teacher.email}
                </span>
              </div>
              {teacher.telephone && (
                <div className="flex items-center space-x-2">
                  <Phone className="h-4 w-4 text-gray-500" />
                  <span className="text-sm text-gray-600">
                    <span className="font-medium">Phone:</span> {teacher.telephone}
                  </span>
                </div>
              )}
            </div>
          </div>
          
          <AutoForm
            recipe={editTeacherRecipe}
            defaultValues={{
              telephone: teacher.telephone || "",
              address: teacher.address || "",
              qualifications: teacher.qualifications,
              subjectsTaught: teacher.subjectsTaught,
              availableHours: teacher.availableHours,
              schedulePreferences: teacher.schedulePreferences,
            }}
            submitLabel="Update Teacher"
            submitClassName="bg-gradient-to-r from-green-600 to-emerald-600 hover:from-green-700 hover:to-emerald-700 shadow-lg hover:shadow-xl transition-all duration-300"
          />
          
          {/* Debug: Show current form state */}
          <div className="mt-4 p-4 bg-blue-50 border border-blue-200 rounded-lg">
            <h4 className="text-sm font-medium text-blue-800 mb-2">Form Debug Info:</h4>
            <div className="text-xs text-blue-700 space-y-1">
              <div>Teacher ID: {teacher.id}</div>
              <div>Current telephone in teacher data: '{teacher.telephone || 'null'}'</div>
              <div>Current address in teacher data: '{teacher.address || 'null'}'</div>
              <div>Form fields count: {teacherUpdateFields.length}</div>
              <div>Form field names: {teacherUpdateFields.map(f => f.name).join(', ')}</div>
            </div>
          </div>
          

        </div>
      </SheetContent>
    </Sheet>
  )
} 