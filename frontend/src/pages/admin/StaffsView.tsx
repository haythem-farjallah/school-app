import * as React from "react";
import { useParams, useNavigate } from "react-router-dom";
import { ArrowLeft, Users2 } from "lucide-react";
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from "@/components/ui/card";
import { Button } from "@/components/ui/button";
import { useStaffById } from "@/features/staff/hooks/use-staff";

export default function StaffsView() {
  const { id } = useParams<{ id: string }>();
  const navigate = useNavigate();
  const staffId = Number(id);
  const { data: staff, isLoading, error } = useStaffById(staffId);

  const handleBack = React.useCallback(() => {
    navigate("/admin/staff");
  }, [navigate]);

  if (isLoading) {
    return <div className="p-8 text-center text-lg">Loading staff member...</div>;
  }
  if (error || !staff) {
    return <div className="p-8 text-center text-red-600">Staff member not found.</div>;
  }

  return (
    <div className="container mx-auto py-8 space-y-8">
      <Card className="border-emerald-200/60 bg-gradient-to-br from-emerald-50/80 via-teal-50/40 to-cyan-50/20 shadow-xl backdrop-blur-sm">
        <CardHeader className="pb-6 flex items-center justify-between">
          <div className="flex items-center space-x-4">
            <Button
              variant="ghost"
              size="sm"
              onClick={handleBack}
              className="text-emerald-700 hover:text-emerald-900 hover:bg-emerald-100/50"
            >
              <ArrowLeft className="mr-2 h-4 w-4" />
              Back to Staff
            </Button>
            <div className="space-y-2">
              <CardTitle className="text-3xl font-bold bg-gradient-to-r from-emerald-900 via-teal-800 to-cyan-700 bg-clip-text text-transparent">
                Staff Member Details
              </CardTitle>
              <CardDescription className="text-emerald-700/80 text-lg">
                View all information about this staff member
              </CardDescription>
            </div>
          </div>
          <div className="p-3 bg-gradient-to-br from-emerald-100 to-teal-100 rounded-lg">
            <Users2 className="h-8 w-8 text-emerald-600" />
          </div>
        </CardHeader>
      </Card>

      <Card className="border-slate-200/60 shadow-xl bg-white/95 backdrop-blur-sm max-w-3xl mx-auto">
        <CardHeader className="border-b border-slate-200/60 bg-gradient-to-r from-slate-50/80 to-emerald-50/40">
          <CardTitle className="text-2xl font-bold text-slate-900">
            {staff.firstName} {staff.lastName}
          </CardTitle>
          <CardDescription className="text-slate-600">
            Staff Type: <span className="font-semibold text-emerald-700">{staff.staffType}</span> | Department: <span className="font-semibold text-emerald-700">{staff.department}</span>
          </CardDescription>
        </CardHeader>
        <CardContent className="p-8 space-y-4">
          <div className="grid grid-cols-1 md:grid-cols-2 gap-6">
            <div>
              <div className="mb-2 text-sm text-slate-500">Email</div>
              <div className="font-medium text-slate-900">{staff.email}</div>
            </div>
            <div>
              <div className="mb-2 text-sm text-slate-500">Phone</div>
              <div className="font-medium text-slate-900">{staff.telephone || <span className="text-slate-400">N/A</span>}</div>
            </div>
            <div>
              <div className="mb-2 text-sm text-slate-500">Birthday</div>
              <div className="font-medium text-slate-900">{staff.birthday ? staff.birthday.substring(0, 10) : <span className="text-slate-400">N/A</span>}</div>
            </div>
            <div>
              <div className="mb-2 text-sm text-slate-500">Gender</div>
              <div className="font-medium text-slate-900">{staff.gender || <span className="text-slate-400">N/A</span>}</div>
            </div>
            <div>
              <div className="mb-2 text-sm text-slate-500">Address</div>
              <div className="font-medium text-slate-900">{staff.address || <span className="text-slate-400">N/A</span>}</div>
            </div>
            <div>
              <div className="mb-2 text-sm text-slate-500">Created At</div>
              <div className="font-medium text-slate-900">{staff.createdAt ? staff.createdAt.substring(0, 10) : <span className="text-slate-400">N/A</span>}</div>
            </div>
          </div>
        </CardContent>
      </Card>
    </div>
  );
} 