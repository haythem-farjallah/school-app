import { useParams, useNavigate } from 'react-router-dom';
import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card';
import { Button } from '@/components/ui/button';
import { Badge } from '@/components/ui/badge';
import { ArrowLeft, Edit, Trash2, User, Mail, Phone, MapPin, Calendar, Users } from 'lucide-react';
import { useParent, useDeleteParent } from '@/features/parents/hooks/use-parents';
import toast from 'react-hot-toast';
import { format } from 'date-fns';

export default function ParentsView() {
  const { id } = useParams<{ id: string }>();
  const navigate = useNavigate();
  const deleteParentMutation = useDeleteParent();

  const { data: parent, isLoading } = useParent(Number(id));

  const handleEdit = () => {
    navigate(`/admin/parents/edit/${id}`);
  };

  const handleDelete = async () => {
    if (!parent) return;

    if (window.confirm(`Are you sure you want to delete ${parent.firstName} ${parent.lastName}?`)) {
      try {
        await deleteParentMutation.mutateAsync(parent.id);
        toast.success('Parent deleted successfully');
        navigate('/admin/parents');
      } catch {
        toast.error('Failed to delete parent');
      }
    }
  };

  if (isLoading) {
    return (
      <div className="container mx-auto py-6">
        <div className="animate-pulse">
          <div className="h-8 bg-gray-200 rounded w-1/4 mb-6"></div>
          <div className="grid gap-6">
            <div className="h-64 bg-gray-200 rounded"></div>
            <div className="h-32 bg-gray-200 rounded"></div>
          </div>
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

  return (
    <div className="container mx-auto py-6">
      <div className="flex items-center justify-between mb-6">
        <div className="flex items-center gap-4">
          <Button
            variant="outline"
            size="sm"
            onClick={() => navigate('/admin/parents')}
            className="flex items-center gap-2"
          >
            <ArrowLeft className="h-4 w-4" />
            Back to Parents
          </Button>
          <div>
            <h1 className="text-3xl font-bold">Parent Details</h1>
            <p className="text-muted-foreground">View parent information</p>
          </div>
        </div>
        <div className="flex gap-2">
          <Button onClick={handleEdit} className="flex items-center gap-2">
            <Edit className="h-4 w-4" />
            Edit
          </Button>
          <Button
            variant="destructive"
            onClick={handleDelete}
            disabled={deleteParentMutation.isPending}
            className="flex items-center gap-2"
          >
            <Trash2 className="h-4 w-4" />
            Delete
          </Button>
        </div>
      </div>

      <div className="grid gap-6">
        {/* Basic Information */}
        <Card>
          <CardHeader>
            <CardTitle className="flex items-center gap-2">
              <User className="h-5 w-5" />
              Basic Information
            </CardTitle>
          </CardHeader>
          <CardContent className="space-y-4">
            <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
              <div>
                <label className="text-sm font-medium text-muted-foreground">First Name</label>
                <p className="text-lg">{parent.firstName}</p>
              </div>
              <div>
                <label className="text-sm font-medium text-muted-foreground">Last Name</label>
                <p className="text-lg">{parent.lastName}</p>
              </div>
              <div>
                <label className="text-sm font-medium text-muted-foreground">Gender</label>
                <div className="flex items-center gap-2 mt-1">
                  <Badge variant={parent.gender === 'M' ? 'default' : 'secondary'}>
                    {parent.gender === 'M' ? 'Male' : parent.gender === 'F' ? 'Female' : parent.gender === 'O' ? 'Other' : parent.gender}
                  </Badge>
                </div>
              </div>
              <div>
                <label className="text-sm font-medium text-muted-foreground">Date of Birth</label>
                <p className="text-lg flex items-center gap-2">
                  <Calendar className="h-4 w-4" />
                  {parent.birthday ? format(new Date(parent.birthday), 'PPP') : 'Not specified'}
                </p>
              </div>
            </div>
          </CardContent>
        </Card>

        {/* Contact Information */}
        <Card>
          <CardHeader>
            <CardTitle className="flex items-center gap-2">
              <Mail className="h-5 w-5" />
              Contact Information
            </CardTitle>
          </CardHeader>
          <CardContent className="space-y-4">
            <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
              <div>
                <label className="text-sm font-medium text-muted-foreground">Email</label>
                <p className="text-lg flex items-center gap-2">
                  <Mail className="h-4 w-4" />
                  {parent.email || 'Not specified'}
                </p>
              </div>
              <div>
                <label className="text-sm font-medium text-muted-foreground">Phone</label>
                <p className="text-lg flex items-center gap-2">
                  <Phone className="h-4 w-4" />
                  {parent.telephone || 'Not specified'}
                </p>
              </div>
              <div className="md:col-span-2">
                <label className="text-sm font-medium text-muted-foreground">Address</label>
                <p className="text-lg flex items-center gap-2">
                  <MapPin className="h-4 w-4" />
                  {parent.address || 'Not specified'}
                </p>
              </div>
            </div>
          </CardContent>
        </Card>

        {/* Children Information */}
        <Card>
          <CardHeader>
            <CardTitle className="flex items-center gap-2">
              <Users className="h-5 w-5" />
              Children
            </CardTitle>
          </CardHeader>
          <CardContent>
            {parent.children && parent.children.length > 0 ? (
              <div className="space-y-3">
                {parent.children.map((child) => (
                  <div key={child.id} className="flex items-center justify-between p-3 border rounded-lg">
                    <div>
                      <p className="font-medium">{child.firstName} {child.lastName}</p>
                      <p className="text-sm text-muted-foreground">Student ID: {child.id}</p>
                      <p className="text-sm text-muted-foreground">Email: {child.email}</p>
                    </div>
                    <Badge variant="outline">{child.gradeLevel || 'No level assigned'}</Badge>
                  </div>
                ))}
              </div>
            ) : (
              <p className="text-muted-foreground">No children assigned to this parent</p>
            )}
          </CardContent>
        </Card>

        {/* System Information */}
        <Card>
          <CardHeader>
            <CardTitle>System Information</CardTitle>
          </CardHeader>
          <CardContent className="space-y-4">
            <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
              <div>
                <label className="text-sm font-medium text-muted-foreground">ID</label>
                <p className="text-sm font-mono">{parent.id}</p>
              </div>
              <div>
                <label className="text-sm font-medium text-muted-foreground">Preferred Contact Method</label>
                <div className="flex items-center gap-2 mt-1">
                  <Badge variant="outline">
                    {parent.preferredContactMethod === 'EMAIL' ? 'Email' : 
                     parent.preferredContactMethod === 'PHONE' ? 'Phone' : 
                     parent.preferredContactMethod === 'SMS' ? 'SMS' : 
                     parent.preferredContactMethod === 'WHATSAPP' ? 'WhatsApp' : 
                     parent.preferredContactMethod === 'IN_PERSON' ? 'In Person' : 
                     parent.preferredContactMethod || 'Not specified'}
                  </Badge>
                </div>
              </div>
              <div>
                <label className="text-sm font-medium text-muted-foreground">Relation</label>
                <p className="text-lg">{parent.relation || 'Not specified'}</p>
              </div>
            </div>
          </CardContent>
        </Card>
      </div>
    </div>
  );
} 