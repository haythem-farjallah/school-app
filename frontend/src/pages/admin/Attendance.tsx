import { Tabs, TabsContent, TabsList, TabsTrigger } from '../../components/ui/tabs';
import { Users, Table, BarChart3 } from 'lucide-react';
import { AttendanceTable } from '../../features/attendance/components/attendance-table';
import { ClassAttendanceMarking } from '../../features/attendance/components/class-attendance-marking';

export default function AttendancePage() {
  return (
    <div className="p-6">
      <div className="mb-6">
        <h1 className="text-3xl font-bold text-gray-900">Attendance Management</h1>
        <p className="text-gray-600 mt-1">
          Manage student attendance across all classes
        </p>
      </div>

      <Tabs defaultValue="quick-marking" className="w-full">
        <TabsList className="grid w-full grid-cols-2">
          <TabsTrigger value="quick-marking" className="flex items-center gap-2">
            <Users className="h-4 w-4" />
            Mark Attendance
          </TabsTrigger>
          <TabsTrigger value="records" className="flex items-center gap-2">
            <Table className="h-4 w-4" />
            Attendance Records
          </TabsTrigger>
        </TabsList>

        <TabsContent value="quick-marking" className="mt-6">
          <ClassAttendanceMarking />
        </TabsContent>

        <TabsContent value="records" className="mt-6">
          <AttendanceTable />
        </TabsContent>
      </Tabs>
    </div>
  );
}
