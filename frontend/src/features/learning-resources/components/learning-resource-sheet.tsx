import * as React from "react"
import { Plus, Library, Edit } from "lucide-react"
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
import type { LearningResource, CreateLearningResourceRequest } from "@/types/learning-resource"
import { ResourceType } from "@/types/learning-resource"
import { 
  learningResourceSchema, 
  learningResourceFields, 
  type LearningResourceValues
} from "../learningResourceForm.definition"

interface AddLearningResourceSheetProps {
  onSuccess?: () => void
}

export function AddLearningResourceSheet({ onSuccess }: AddLearningResourceSheetProps) {
  const [open, setOpen] = React.useState(false)

  // URL Resource Creation
  const addUrlResourceMutation = useMutationApi<LearningResource, CreateLearningResourceRequest>(
    async (data) => {
      const response = await http.post<LearningResource>("/v1/learning-resources", data)
      return response.data
    },
    {
      onSuccess: () => {
        toast.success("Learning resource created successfully!")
        setOpen(false)
        onSuccess?.()
      },
      onError: (error: unknown) => {
        const message = error && typeof error === 'object' && 'response' in error && 
          error.response && typeof error.response === 'object' && 'data' in error.response &&
          error.response.data && typeof error.response.data === 'object' && 'message' in error.response.data
          ? String(error.response.data.message) : "Failed to create learning resource"
        toast.error(message)
      },
    }
  )

  const urlResourceRecipe: FormRecipe = {
    schema: learningResourceSchema,
    fields: learningResourceFields,
    onSubmit: async (values: unknown) => {
      const formValues = values as LearningResourceValues
      const resourceData: CreateLearningResourceRequest = {
        title: formValues.title,
        description: formValues.description,
        url: formValues.url,
        type: formValues.type,
        thumbnailUrl: formValues.thumbnailUrl,
        duration: formValues.duration,
        isPublic: formValues.isPublic,
      }
      await addUrlResourceMutation.mutateAsync(resourceData)
    },
  }

  return (
    <Sheet open={open} onOpenChange={setOpen}>
      <SheetTrigger asChild>
        <Button className="bg-gradient-to-r from-indigo-600 to-purple-600 hover:from-indigo-700 hover:to-purple-700 shadow-lg hover:shadow-xl transition-all duration-300 transform hover:scale-105">
          <Plus className="mr-2 h-4 w-4" />
          Add Learning Resource
        </Button>
      </SheetTrigger>
      <SheetContent className="w-[400px] sm:w-[540px] bg-white border-l-4 border-indigo-500 overflow-y-auto max-h-screen">
        <SheetHeader className="space-y-4 pb-6 border-b border-gray-100">
          <div className="flex items-center space-x-3">
            <div className="p-2 bg-gradient-to-br from-indigo-100 to-purple-100 rounded-lg">
              <Library className="h-6 w-6 text-indigo-600" />
            </div>
            <div>
              <SheetTitle className="text-2xl font-bold bg-gradient-to-r from-indigo-900 to-purple-700 bg-clip-text text-transparent">
                Add Learning Resource
              </SheetTitle>
              <SheetDescription className="text-gray-600">
                Create a new learning resource from URL or upload a file
              </SheetDescription>
            </div>
          </div>
        </SheetHeader>

        <div className="py-6">
          <div className="text-sm text-gray-600 mb-4">
            Create a learning resource from an external URL (videos, documents, etc.)
          </div>
          <AutoForm
            recipe={urlResourceRecipe}
            defaultValues={{
              title: "",
              description: "",
              url: "",
              type: ResourceType.VIDEO,
              thumbnailUrl: "",
              duration: 0,
              isPublic: true,
            }}
            submitLabel="Create Resource"
            submitClassName="bg-gradient-to-r from-indigo-600 to-purple-600 hover:from-indigo-700 hover:to-purple-700 shadow-lg hover:shadow-xl transition-all duration-300"
          />
        </div>
      </SheetContent>
    </Sheet>
  )
}

interface EditLearningResourceSheetProps {
  resource: LearningResource
  trigger?: React.ReactNode
  onSuccess?: () => void
}

export function EditLearningResourceSheet({ resource, trigger, onSuccess }: EditLearningResourceSheetProps) {
  const [open, setOpen] = React.useState(false)

  const editResourceMutation = useMutationApi<LearningResource, LearningResourceValues>(
    async (data) => {
      const response = await http.put<LearningResource>(`/v1/learning-resources/${resource.id}`, data)
      return response.data
    },
    {
      onSuccess: () => {
        toast.success("Learning resource updated successfully!")
        setOpen(false)
        onSuccess?.()
      },
      onError: (error: unknown) => {
        const message = error && typeof error === 'object' && 'response' in error && 
          error.response && typeof error.response === 'object' && 'data' in error.response &&
          error.response.data && typeof error.response.data === 'object' && 'message' in error.response.data
          ? String(error.response.data.message) : "Failed to update learning resource"
        toast.error(message)
      },
    }
  )

  const editResourceRecipe: FormRecipe = {
    schema: learningResourceSchema,
    fields: learningResourceFields,
    onSubmit: async (values: unknown) => {
      await editResourceMutation.mutateAsync(values as LearningResourceValues)
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
                Edit Learning Resource
              </SheetTitle>
              <SheetDescription className="text-gray-600">
                Update learning resource information and settings
              </SheetDescription>
            </div>
          </div>
        </SheetHeader>

        <div className="py-6">
          <AutoForm
            recipe={editResourceRecipe}
            defaultValues={{
              title: resource.title,
              description: resource.description,
              url: resource.url || "",
              type: resource.type,
              thumbnailUrl: resource.thumbnailUrl || "",
              duration: resource.duration || 0,
              isPublic: resource.isPublic,
            }}
            submitLabel="Update Resource"
            submitClassName="bg-gradient-to-r from-orange-600 to-amber-600 hover:from-orange-700 hover:to-amber-700 shadow-lg hover:shadow-xl transition-all duration-300"
          />
        </div>
      </SheetContent>
    </Sheet>
  )
} 