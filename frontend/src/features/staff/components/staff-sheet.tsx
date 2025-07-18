import * as React from "react"
import { Plus, Users2, Edit } from "lucide-react"
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
import type { Staff, CreateStaffRequest } from "@/types/staff"
import { staffSchema, staffFields, type StaffValues } from "../staffForm.definition"

interface AddStaffSheetProps {
  onSuccess?: () => void
}

export function AddStaffSheet({ onSuccess }: AddStaffSheetProps) {
  const [open, setOpen] = React.useState(false)

  const addStaffMutation = useMutationApi<Staff, CreateStaffRequest>(
    async (data) => {
      console.log("➕ AddStaffSheet - Creating staff:", data);
      const response = await http.post<{ status: string; data: Staff }>("/admin/staff", data)
      console.log("➕ AddStaffSheet - Response:", response.data);
      return response.data.data
    },
    {
      onSuccess: (data) => {
        console.log("✅ AddStaffSheet - Staff created successfully:", data);
        toast.success("Staff member added successfully!")
        setOpen(false)
        onSuccess?.()
      },
      onError: (error: unknown) => {
        console.error("❌ AddStaffSheet - Failed to create staff:", error);
        const message = error && typeof error === 'object' && 'response' in error && 
          error.response && typeof error.response === 'object' && 'data' in error.response &&
          error.response.data && typeof error.response.data === 'object' && 'message' in error.response.data
          ? String(error.response.data.message) : "Failed to add staff member"
        toast.error(message)
      },
    }
  )

  const addStaffRecipe: FormRecipe = {
    schema: staffSchema,
    fields: staffFields,
    onSubmit: async (values: unknown) => {
      console.log("➕ AddStaffSheet - Form submitted with values:", values);
      const formValues = values as StaffValues;
      const staffData: CreateStaffRequest = {
        profile: {
          firstName: formValues.firstName,
          lastName: formValues.lastName,
          email: formValues.email,
          telephone: formValues.telephone || "",
          birthday: formValues.birthday || "",
          gender: (formValues.gender as 'M' | 'F' | 'O') || 'M',
          address: formValues.address || "",
          role: 'STAFF',
        },
        staffType: formValues.staffType,
        department: formValues.department,
      };
      console.log("➕ AddStaffSheet - Transformed staff data:", staffData);
      await addStaffMutation.mutateAsync(staffData);
    },
  }

  return (
    <Sheet open={open} onOpenChange={setOpen}>
      <SheetTrigger asChild>
        <Button className="bg-gradient-to-r from-emerald-600 to-teal-600 hover:from-emerald-700 hover:to-teal-700 shadow-lg hover:shadow-xl transition-all duration-300 transform hover:scale-105">
          <Plus className="mr-2 h-4 w-4" />
          Add Staff Member
        </Button>
      </SheetTrigger>
      <SheetContent className="w-[400px] sm:w-[540px] bg-white border-l-4 border-emerald-500 overflow-y-auto max-h-screen">
        <SheetHeader className="space-y-4 pb-6 border-b border-gray-100">
          <div className="flex items-center space-x-3">
            <div className="p-2 bg-gradient-to-br from-emerald-100 to-teal-100 rounded-lg">
              <Users2 className="h-6 w-6 text-emerald-600" />
            </div>
            <div>
              <SheetTitle className="text-2xl font-bold bg-gradient-to-r from-emerald-900 to-teal-700 bg-clip-text text-transparent">
                Add New Staff Member
              </SheetTitle>
              <SheetDescription className="text-gray-600">
                Create a new staff member profile with all the necessary details
              </SheetDescription>
            </div>
          </div>
        </SheetHeader>

        <div className="py-6">
          <AutoForm
            recipe={addStaffRecipe}
            defaultValues={{
              firstName: "",
              lastName: "",
              email: "",
              telephone: "",
              birthday: "",
              gender: "",
              address: "",
              staffType: "ADMINISTRATIVE",
              department: "",
            }}
            submitLabel="Add Staff Member"
            submitClassName="bg-gradient-to-r from-emerald-600 to-teal-600 hover:from-emerald-700 hover:to-teal-700 shadow-lg hover:shadow-xl transition-all duration-300"
          />
        </div>
      </SheetContent>
    </Sheet>
  )
}

interface EditStaffSheetProps {
  staff: Staff
  trigger?: React.ReactNode
  onSuccess?: () => void
}

export function EditStaffSheet({ staff, trigger, onSuccess }: EditStaffSheetProps) {
  const [open, setOpen] = React.useState(false)

  const editStaffMutation = useMutationApi<Staff, StaffValues>(
    async (data) => {
      console.log("✏️ EditStaffSheet - Updating staff:", data);
      const response = await http.patch<{ status: string; data: Staff }>(`/admin/staff/${staff.id}`, data)
      console.log("✏️ EditStaffSheet - Response:", response.data);
      return response.data.data
    },
    {
      onSuccess: (data) => {
        console.log("✅ EditStaffSheet - Staff updated successfully:", data);
        toast.success("Staff member updated successfully!")
        setOpen(false)
        onSuccess?.()
      },
      onError: (error: unknown) => {
        console.error("❌ EditStaffSheet - Failed to update staff:", error);
        const message = error && typeof error === 'object' && 'response' in error && 
          error.response && typeof error.response === 'object' && 'data' in error.response &&
          error.response.data && typeof error.response.data === 'object' && 'message' in error.response.data
          ? String(error.response.data.message) : "Failed to update staff member"
        toast.error(message)
      },
    }
  )

  const editStaffRecipe: FormRecipe = {
    schema: staffSchema,
    fields: staffFields,
    onSubmit: async (values: unknown) => {
      console.log("✏️ EditStaffSheet - Form submitted with values:", values);
      await editStaffMutation.mutateAsync(values as StaffValues)
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
      <SheetContent className="w-[400px] sm:w-[540px] bg-white border-l-4 border-orange-500 overflow-y-auto max-h-screen">
        <SheetHeader className="space-y-4 pb-6 border-b border-gray-100">
          <div className="flex items-center space-x-3">
            <div className="p-2 bg-gradient-to-br from-orange-100 to-amber-100 rounded-lg">
              <Edit className="h-6 w-6 text-orange-600" />
            </div>
            <div>
              <SheetTitle className="text-2xl font-bold bg-gradient-to-r from-orange-900 to-amber-700 bg-clip-text text-transparent">
                Edit Staff Member
              </SheetTitle>
              <SheetDescription className="text-gray-600">
                Update staff member information and settings
              </SheetDescription>
            </div>
          </div>
        </SheetHeader>

        <div className="py-6">
          <AutoForm
            recipe={editStaffRecipe}
            defaultValues={{
              firstName: staff.firstName,
              lastName: staff.lastName,
              email: staff.email,
              telephone: staff.telephone || "",
              birthday: staff.birthday || "",
              gender: staff.gender || "",
              address: staff.address || "",
              staffType: staff.staffType,
              department: staff.department,
            }}
            submitLabel="Update Staff Member"
            submitClassName="bg-gradient-to-r from-orange-600 to-amber-600 hover:from-orange-700 hover:to-amber-700 shadow-lg hover:shadow-xl transition-all duration-300"
          />
        </div>
      </SheetContent>
    </Sheet>
  )
} 