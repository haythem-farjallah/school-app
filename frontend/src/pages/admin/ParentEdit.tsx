import { useParams, useNavigate } from 'react-router-dom';
import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card';
import { Button } from '@/components/ui/button';
import { ArrowLeft } from 'lucide-react';
import { useParent, useUpdateParent } from '@/features/parents/hooks/use-parents';
import { AutoForm } from '@/form/AutoForm';
import type { FormRecipe } from '@/form/types';
import { parentUpdateSchema, parentUpdateFields, type ParentUpdateValues } from '@/features/parents/parentForm.definition';
import type { UpdateParentData } from '@/types/parent';
import type { Student } from '@/types/student';
import toast from 'react-hot-toast';

export default function ParentEdit() {
  const { id } = useParams<{ id: string }>();
  const navigate = useNavigate();
  const updateParentMutation = useUpdateParent();

  const { data: parent, isLoading } = useParent(Number(id));

  const updateParentRecipe: FormRecipe = {
    schema: parentUpdateSchema,
    fields: parentUpdateFields,
    onSubmit: async (values: unknown) => {
      if (!parent) return;

      const formValues = values as ParentUpdateValues;
      
      try {
        const parentData: UpdateParentData & { id: number } = {
          id: parent.id,
          telephone: formValues.telephone && formValues.telephone.trim() !== "" ? formValues.telephone : null,
          address: formValues.address && formValues.address.trim() !== "" ? formValues.address : null,
          preferredContactMethod: formValues.preferredContactMethod,
          relation: formValues.relation,
          children: formValues.children?.length ? formValues.children.map((child: Student) => child.email) : undefined,
        };

        await updateParentMutation.mutateAsync(parentData);
        toast.success("Parent updated successfully!");
        navigate(`/admin/parents/view/${id}`);
      } catch (error) {
        toast.error("Failed to update parent");
      }
    },
  };

  if (isLoading) {
    return (
      <div className="container mx-auto py-6">
        <div className="animate-pulse">
          <div className="h-8 bg-gray-200 rounded w-1/4 mb-6"></div>
          <div className="h-96 bg-gray-200 rounded"></div>
        </div>
      </div>
    );
  }

  if (!parent) {
    return (
      <div className="container mx-auto py-6">
        <Card>
          <CardContent className="pt-6">
            <div className="text-center">
              <p className="text-muted-foreground">Parent not found</p>
              <Button onClick={() => navigate('/admin/parents')} className="mt-4">
                Back to Parents
              </Button>
            </div>
          </CardContent>
        </Card>
      </div>
    );
  }

  const defaultValues: ParentUpdateValues = {
    telephone: parent.telephone || '',
    address: parent.address || '',
    preferredContactMethod: parent.preferredContactMethod,
    relation: parent.relation,
    children: parent.children || [],
  };

  return (
    <div className="container mx-auto py-6">
      <div className="flex items-center gap-4 mb-6">
        <Button
          variant="outline"
          size="sm"
          onClick={() => navigate(`/admin/parents/view/${id}`)}
          className="flex items-center gap-2"
        >
          <ArrowLeft className="h-4 w-4" />
          Back to Details
        </Button>
        <div>
          <h1 className="text-3xl font-bold">Edit Parent</h1>
          <p className="text-muted-foreground">
            Update information for {parent.firstName} {parent.lastName}
          </p>
        </div>
      </div>

      <Card>
        <CardHeader>
          <CardTitle>Parent Information</CardTitle>
        </CardHeader>
        <CardContent>
          <AutoForm
            recipe={updateParentRecipe}
            defaultValues={defaultValues}
            submitLabel="Update Parent"
            submitClassName="w-full bg-gradient-to-r from-blue-600 to-purple-600 hover:from-blue-700 hover:to-purple-700 shadow-lg hover:shadow-xl transition-all duration-300"
          />
        </CardContent>
      </Card>
    </div>
  );
}
