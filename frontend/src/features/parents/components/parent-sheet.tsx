import * as React from "react"
import { Plus, User2, Edit } from "lucide-react"
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
import type { Parent, CreateParentData } from "@/types/parent"
import { parentSchema, parentFields, type ParentValues } from "../parentForm.definition"

interface AddParentSheetProps {
  onSuccess?: () => void
}

export function AddParentSheet({ onSuccess }: AddParentSheetProps) {
  const [open, setOpen] = React.useState(false)

  const addParentMutation = useMutationApi<Parent, CreateParentData>(
    async (data) => {
      const response = await http.post<Parent>("/admin/parents", data)
      return response.data
    },
    {
      onSuccess: () => {
        toast.success("Parent added successfully!")
        setOpen(false)
        onSuccess?.()
      },
      onError: (error: unknown) => {
        const message = error && typeof error === 'object' && 'response' in error && 
          error.response && typeof error.response === 'object' && 'data' in error.response &&
          error.response.data && typeof error.response.data === 'object' && 'message' in error.response.data
          ? String(error.response.data.message) : "Failed to add parent"
        toast.error(message)
      },
    }
  )

  const addParentRecipe: FormRecipe = {
    schema: parentSchema,
    fields: parentFields,
    onSubmit: async (values: unknown) => {
      const formValues = values as ParentValues;
      const parentData: CreateParentData = {
        profile: {
          firstName: formValues.firstName,
          lastName: formValues.lastName,
          email: formValues.email,
          telephone: formValues.telephone,
          birthday: formValues.birthday,
          gender: formValues.gender,
          address: formValues.address,
        },
        preferredContactMethod: formValues.preferredContactMethod,
      };
      await addParentMutation.mutateAsync(parentData);
    },
  }

  return (
    <Sheet open={open} onOpenChange={setOpen}>
      <SheetTrigger asChild>
        <Button className="bg-gradient-to-r from-purple-600 to-pink-600 hover:from-purple-700 hover:to-pink-700 shadow-lg hover:shadow-xl transition-all duration-300">
          <Plus className="mr-2 h-4 w-4" />
          Add Parent
        </Button>
      </SheetTrigger>
      <SheetContent className="w-[400px] sm:w-[540px] bg-gradient-to-br from-white to-purple-50 border-l-4 border-purple-500">
        <SheetHeader className="pb-6 border-b border-purple-100">
          <div className="flex items-center gap-3">
            <div className="p-2 bg-gradient-to-br from-purple-500 to-pink-600 rounded-lg">
              <User2 className="h-5 w-5 text-white" />
            </div>
            <div>
              <SheetTitle className="text-xl font-bold bg-gradient-to-r from-purple-800 to-pink-800 bg-clip-text text-transparent">
                Add New Parent
              </SheetTitle>
              <SheetDescription className="text-gray-600">
                Fill in the parent information below to add them to the system.
              </SheetDescription>
            </div>
          </div>
        </SheetHeader>
        <div className="py-6">
          <AutoForm
            recipe={addParentRecipe}
            submitLabel="Add Parent"
            submitClassName="bg-gradient-to-r from-purple-600 to-pink-600 hover:from-purple-700 hover:to-pink-700 shadow-lg hover:shadow-xl transition-all duration-300"
          />
        </div>
      </SheetContent>
    </Sheet>
  )
}

interface EditParentSheetProps {
  parent: Parent
  trigger?: React.ReactNode
  onSuccess?: () => void
}

export function EditParentSheet({ parent, trigger, onSuccess }: EditParentSheetProps) {
  const [open, setOpen] = React.useState(false)

  const editParentMutation = useMutationApi<Parent, CreateParentData>(
    async (data) => {
      const response = await http.patch<Parent>(`/admin/parents/${parent.id}`, data)
      return response.data
    },
    {
      onSuccess: () => {
        toast.success("Parent updated successfully!")
        setOpen(false)
        onSuccess?.()
      },
      onError: (error: unknown) => {
        const message = error && typeof error === 'object' && 'response' in error && 
          error.response && typeof error.response === 'object' && 'data' in error.response &&
          error.response.data && typeof error.response.data === 'object' && 'message' in error.response.data
          ? String(error.response.data.message) : "Failed to update parent"
        toast.error(message)
      },
    }
  )

  const editParentRecipe: FormRecipe = {
    schema: parentSchema,
    fields: parentFields,
    onSubmit: async (values: unknown) => {
      const formValues = values as ParentValues;
      const parentData: CreateParentData = {
        profile: {
          firstName: formValues.firstName,
          lastName: formValues.lastName,
          email: formValues.email,
          telephone: formValues.telephone,
          birthday: formValues.birthday,
          gender: formValues.gender,
          address: formValues.address,
        },
        preferredContactMethod: formValues.preferredContactMethod,
      };
      await editParentMutation.mutateAsync(parentData);
    },
  }

  return (
    <Sheet open={open} onOpenChange={setOpen}>
      <SheetTrigger asChild>
        {trigger || (
          <Button variant="ghost" size="sm" className="hover:bg-purple-50 hover:text-purple-700 transition-colors duration-200">
            <Edit className="h-4 w-4" />
          </Button>
        )}
      </SheetTrigger>
      <SheetContent className="w-[400px] sm:w-[540px] bg-gradient-to-br from-white to-green-50 border-l-4 border-green-500">
        <SheetHeader className="pb-6 border-b border-green-100">
          <div className="flex items-center gap-3">
            <div className="p-2 bg-gradient-to-br from-green-500 to-emerald-600 rounded-lg">
              <Edit className="h-5 w-5 text-white" />
            </div>
            <div>
              <SheetTitle className="text-xl font-bold bg-gradient-to-r from-green-800 to-emerald-800 bg-clip-text text-transparent">
                Edit Parent
              </SheetTitle>
              <SheetDescription className="text-gray-600">
                Update the parent information below.
              </SheetDescription>
            </div>
          </div>
        </SheetHeader>
        <div className="py-6">
          <AutoForm
            recipe={editParentRecipe}
            defaultValues={{
              firstName: parent.firstName,
              lastName: parent.lastName,
              email: parent.email,
              telephone: parent.telephone || "",
              birthday: parent.birthday || "",
              gender: parent.gender || "",
              address: parent.address || "",
              preferredContactMethod: parent.preferredContactMethod || "",
            }}
            submitLabel="Update Parent"
            submitClassName="bg-gradient-to-r from-green-600 to-emerald-600 hover:from-green-700 hover:to-emerald-700 shadow-lg hover:shadow-xl transition-all duration-300"
          />
        </div>
      </SheetContent>
    </Sheet>
  )
} 