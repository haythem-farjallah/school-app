import * as React from "react"
import { User2, Edit, Mail, Phone } from "lucide-react"
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
import { useUpdateParent } from "../hooks/use-parents"
import type { Parent, UpdateParentData } from "@/types/parent"
import { parentUpdateSchema, parentUpdateFields, type ParentUpdateValues } from "../parentForm.definition"
import type { Student } from "@/types/student";


interface EditParentSheetProps {
  parent: Parent
  trigger?: React.ReactNode
  onSuccess?: () => void
}

export function EditParentSheet({ parent, trigger, onSuccess }: EditParentSheetProps) {
  const [open, setOpen] = React.useState(false)

  const editParentMutation = useUpdateParent()

  // Debug: Log the parent data and form fields
  React.useEffect(() => {
    if (open) {
      console.log("🔍 EditParentSheet - Parent data:", parent);
      console.log("🔍 EditParentSheet - Update fields:", parentUpdateFields);
      console.log("🔍 EditParentSheet - Default values:", {
        telephone: parent.telephone || "",
        address: parent.address || "",
        preferredContactMethod: parent.preferredContactMethod,
      });
    }
  }, [open, parent]);

  const editParentRecipe: FormRecipe = {
    schema: parentUpdateSchema,
    fields: parentUpdateFields,
    onSubmit: async (values: unknown) => {
      const formValues = values as ParentUpdateValues;
      
      // Debug: Log the raw form values
      console.log("🔍 EditParentSheet - Raw form values:", formValues);
      console.log("🔍 EditParentSheet - Telephone value:", formValues.telephone, "Type:", typeof formValues.telephone);
      console.log("🔍 EditParentSheet - Address value:", formValues.address, "Type:", typeof formValues.address);
      
      // For updates, send the data in the format expected by ParentUpdateDto (flat structure)
      const parentData: UpdateParentData & { id: number } = {
        id: parent.id,
        // Send fields directly (not nested in profile) as expected by ParentUpdateDto
        telephone: formValues.telephone && formValues.telephone.trim() !== "" ? formValues.telephone : null,
        address: formValues.address && formValues.address.trim() !== "" ? formValues.address : null,
        preferredContactMethod: formValues.preferredContactMethod,
        relation: formValues.relation,
        // Send children as array of emails if present, else undefined
        children: formValues.children?.length ? formValues.children.map((child: Student) => child.email) : undefined,
      };
      console.log("✏️ EditParentSheet - Transformed data:", parentData)
      await editParentMutation.mutateAsync(parentData)
    },
  }

  // Handle success and error states with useCallback to prevent infinite loops
  const handleSuccess = React.useCallback(() => {
    toast.success("Parent updated successfully!")
    setOpen(false)
    // Call onSuccess callback if provided
    if (onSuccess) {
      onSuccess()
    }
    // Reset the mutation state to prevent infinite loops
    editParentMutation.reset()
  }, [onSuccess, editParentMutation])

  const handleError = React.useCallback(() => {
    console.error("❌ EditParentSheet - Update failed:", editParentMutation.error)
    const error = editParentMutation.error as { response?: { data?: { message?: string } } };
    const message = error?.response?.data?.message || "Failed to update parent"
    toast.error(message)
    // Reset the mutation state to prevent infinite loops
    editParentMutation.reset()
  }, [editParentMutation])

  React.useEffect(() => {
    if (editParentMutation.isSuccess) {
      handleSuccess()
    }
  }, [editParentMutation.isSuccess, handleSuccess])

  React.useEffect(() => {
    if (editParentMutation.isError) {
      handleError()
    }
  }, [editParentMutation.isError, handleError])

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
                Edit Parent
              </SheetTitle>
              <SheetDescription className="text-gray-600">
                Update parent information and settings
              </SheetDescription>
            </div>
          </div>
        </SheetHeader>

        <div className="py-6">
          {/* Display non-editable fields as read-only information */}
          <div className="mb-6 p-4 bg-gray-50 rounded-lg border border-gray-200">
            <h3 className="text-sm font-medium text-gray-700 mb-3">Parent Information (Read-only)</h3>
            <div className="grid grid-cols-1 gap-3">
              <div className="flex items-center space-x-2">
                <User2 className="h-4 w-4 text-gray-500" />
                <span className="text-sm text-gray-600">
                  <span className="font-medium">Name:</span> {parent.firstName} {parent.lastName}
                </span>
              </div>
              <div className="flex items-center space-x-2">
                <Mail className="h-4 w-4 text-gray-500" />
                <span className="text-sm text-gray-600">
                  <span className="font-medium">Email:</span> {parent.email}
                </span>
              </div>
              {parent.telephone && (
                <div className="flex items-center space-x-2">
                  <Phone className="h-4 w-4 text-gray-500" />
                  <span className="text-sm text-gray-600">
                    <span className="font-medium">Phone:</span> {parent.telephone}
                  </span>
                </div>
              )}
            </div>
          </div>
          
          <AutoForm
            recipe={editParentRecipe}
            defaultValues={{
              telephone: parent.telephone || "",
              address: parent.address || "",
              preferredContactMethod: parent.preferredContactMethod || "",
              relation: parent.relation || "",
              // Pre-fill children as array of student objects (if available)
              children: parent.children || [],
            }}
            submitLabel="Update Parent"
            submitClassName="bg-gradient-to-r from-green-600 to-emerald-600 hover:from-green-700 hover:to-emerald-700 shadow-lg hover:shadow-xl transition-all duration-300"
          />
          
          {/* Debug: Show current form state */}
          <div className="mt-4 p-4 bg-blue-50 border border-blue-200 rounded-lg">
            <h4 className="text-sm font-medium text-blue-800 mb-2">Form Debug Info:</h4>
            <div className="text-xs text-blue-700 space-y-1">
              <div>Parent ID: {parent.id}</div>
              <div>Current telephone in parent data: '{parent.telephone || 'null'}'</div>
              <div>Current address in parent data: '{parent.address || 'null'}'</div>
              <div>Form fields count: {parentUpdateFields.length}</div>
              <div>Form field names: {parentUpdateFields.map(f => f.name).join(', ')}</div>
            </div>
          </div>
        </div>
      </SheetContent>
    </Sheet>
  )
} 