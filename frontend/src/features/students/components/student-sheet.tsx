import * as React from "react"
import { Plus, User, Edit, Calendar } from "lucide-react"
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
import { Input } from "@/components/ui/input"
import { Label } from "@/components/ui/label"
import { Textarea } from "@/components/ui/textarea"
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from "@/components/ui/select"

import {
  Popover,
  PopoverContent,
  PopoverTrigger,
} from "@/components/ui/popover"
import { Calendar as CalendarComponent } from "@/components/ui/calendar"
import { AutoForm } from "@/form/AutoForm"
import type { FormRecipe } from "@/form/types"
import { useMutationApi } from "@/hooks/useMutationApi"
import { http } from "@/lib/http"
import type { Student, CreateStudentData } from "@/types/student"
import { studentSchema, studentFields, type StudentValues } from "../studentForm.definition"
import { format } from "date-fns"
import { cn } from "@/lib/utils"

interface AddStudentSheetProps {
  onSuccess?: () => void
}

export function AddStudentSheet({ onSuccess }: AddStudentSheetProps) {
  const [open, setOpen] = React.useState(false)

  const addStudentMutation = useMutationApi<Student, CreateStudentData>(
    async (data) => {
      const response = await http.post<Student>("/admin/students", data)
      return response.data
    },
    {
      onSuccess: () => {
        toast.success("Student added successfully!")
        setOpen(false)
        onSuccess?.()
      },
      onError: (error: unknown) => {
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
        gradeLevel: formValues.gradeLevel,
        enrollmentYear: formValues.enrollmentYear,
      };
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
  const [formData, setFormData] = React.useState({
    firstName: student.firstName || "",
    lastName: student.lastName || "",
    email: student.email || "",
    telephone: student.telephone || "",
    birthday: student.birthday || "",
    gender: student.gender || "",
    address: student.address || "",
    gradeLevel: student.gradeLevel || "",
    enrollmentYear: student.enrollmentYear || new Date().getFullYear(),
  })
  const [birthdayDate, setBirthdayDate] = React.useState<Date | undefined>(
    student.birthday ? new Date(student.birthday) : undefined
  )
  const [calendarOpen, setCalendarOpen] = React.useState(false)

  const editStudentMutation = useMutationApi<Student, CreateStudentData>(
    async (data) => {
      const response = await http.patch<Student>(`/admin/students/${student.id}`, data)
      return response.data
    },
    {
      onSuccess: () => {
        toast.success("Student updated successfully!")
        setOpen(false)
        onSuccess?.()
      },
      onError: (error: unknown) => {
        const message = error && typeof error === 'object' && 'response' in error && 
          error.response && typeof error.response === 'object' && 'data' in error.response &&
          error.response.data && typeof error.response.data === 'object' && 'message' in error.response.data
          ? String(error.response.data.message) : "Failed to update student"
        toast.error(message)
      },
    }
  )

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault()
    
    const studentData: CreateStudentData = {
      profile: {
        firstName: formData.firstName,
        lastName: formData.lastName,
        email: formData.email,
        telephone: formData.telephone,
        birthday: formData.birthday,
        gender: formData.gender,
        address: formData.address,
      },
      gradeLevel: formData.gradeLevel,
      enrollmentYear: formData.enrollmentYear,
    }
    
    await editStudentMutation.mutateAsync(studentData)
  }

  const handleDateSelect = (date: Date | undefined) => {
    setBirthdayDate(date)
    if (date) {
      setFormData(prev => ({
        ...prev,
        birthday: format(date, "yyyy-MM-dd")
      }))
    }
    setCalendarOpen(false)
  }

  return (
    <Sheet open={open} onOpenChange={setOpen}>
      <SheetTrigger asChild>
        {trigger || (
          <Button variant="ghost" size="sm" className="hover:bg-blue-50 hover:text-blue-700 transition-colors duration-200">
            <Edit className="h-4 w-4" />
          </Button>
        )}
      </SheetTrigger>
      <SheetContent className="w-[90vw] max-w-6xl p-0 bg-gradient-to-br from-white to-green-50/30 border-l-4 border-green-500 overflow-hidden">
        <SheetHeader className="p-6 pb-4 border-b border-green-100/60 bg-gradient-to-r from-green-50/50 to-emerald-50/30">
          <div className="flex items-center justify-between">
            <div className="flex items-center gap-3">
              <div className="p-3 bg-gradient-to-br from-green-500 to-emerald-600 rounded-xl shadow-lg">
                <Edit className="h-6 w-6 text-white" />
              </div>
              <div>
                <SheetTitle className="text-2xl font-bold bg-gradient-to-r from-green-800 to-emerald-800 bg-clip-text text-transparent">
                  Edit Student
                </SheetTitle>
                <SheetDescription className="text-gray-600 text-sm">
                  Update {student.firstName} {student.lastName}'s information
                </SheetDescription>
              </div>
            </div>
          </div>
        </SheetHeader>

        <form onSubmit={handleSubmit} className="flex flex-col h-[calc(100vh-120px)]">
          <div className="flex-1 p-8 overflow-y-auto">
            <div className="max-w-4xl mx-auto">
              {/* All Fields in Single Section */}
              <div className="bg-gradient-to-r from-green-50/80 to-emerald-50/40 p-8 rounded-xl border border-green-200/50 shadow-sm">
                <h3 className="text-xl font-semibold text-green-900 mb-8 flex items-center gap-2">
                  <User className="h-6 w-6" />
                  Student Information
                </h3>
                
                <div className="space-y-6">
                  {/* Row 1: Names */}
                  <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
                    <div>
                      <Label htmlFor="firstName" className="text-sm font-medium text-gray-700">First Name</Label>
                      <Input
                        id="firstName"
                        value={formData.firstName}
                        onChange={(e) => setFormData(prev => ({ ...prev, firstName: e.target.value }))}
                        className="mt-2 bg-white/80 border-green-200 focus:border-green-400 focus:ring-green-200"
                        required
                      />
                    </div>
                    
                    <div>
                      <Label htmlFor="lastName" className="text-sm font-medium text-gray-700">Last Name</Label>
                      <Input
                        id="lastName"
                        value={formData.lastName}
                        onChange={(e) => setFormData(prev => ({ ...prev, lastName: e.target.value }))}
                        className="mt-2 bg-white/80 border-green-200 focus:border-green-400 focus:ring-green-200"
                        required
                      />
                    </div>
                  </div>

                  {/* Row 2: Email */}
                  <div>
                    <Label htmlFor="email" className="text-sm font-medium text-gray-700">Email Address</Label>
                    <Input
                      id="email"
                      type="email"
                      value={formData.email}
                      onChange={(e) => setFormData(prev => ({ ...prev, email: e.target.value }))}
                      className="mt-2 bg-white/80 border-green-200 focus:border-green-400 focus:ring-green-200"
                      required
                    />
                  </div>

                  {/* Row 3: Phone and Birthday */}
                  <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
                    <div>
                      <Label htmlFor="telephone" className="text-sm font-medium text-gray-700">Phone Number</Label>
                      <Input
                        id="telephone"
                        value={formData.telephone}
                        onChange={(e) => setFormData(prev => ({ ...prev, telephone: e.target.value }))}
                        className="mt-2 bg-white/80 border-green-200 focus:border-green-400 focus:ring-green-200"
                      />
                    </div>

                    <div className="relative">
                      <Label htmlFor="birthday" className="text-sm font-medium text-gray-700">Birthday</Label>
                      <Popover open={calendarOpen} onOpenChange={setCalendarOpen}>
                        <PopoverTrigger asChild>
                          <Button
                            variant="outline"
                            className={cn(
                              "w-full mt-2 justify-start text-left font-normal bg-white/80 border-green-200 hover:border-green-400",
                              !birthdayDate && "text-muted-foreground"
                            )}
                          >
                            <Calendar className="mr-2 h-4 w-4 text-green-500" />
                            {birthdayDate ? format(birthdayDate, "MMM d, yyyy") : "Pick a date"}
                          </Button>
                        </PopoverTrigger>
                        <PopoverContent className="w-auto p-0 z-50" align="start" side="bottom">
                          <CalendarComponent
                            mode="single"
                            selected={birthdayDate}
                            onSelect={handleDateSelect}
                            disabled={(date) =>
                              date > new Date() || date < new Date("1900-01-01")
                            }
                            initialFocus
                          />
                        </PopoverContent>
                      </Popover>
                    </div>
                  </div>

                  {/* Row 4: Gender */}
                  <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
                    <div>
                      <Label htmlFor="gender" className="text-sm font-medium text-gray-700">Gender</Label>
                      <Select 
                        value={formData.gender || student.gender || ""} 
                        onValueChange={(value) => setFormData(prev => ({ ...prev, gender: value }))}
                      >
                        <SelectTrigger className="mt-2 bg-white/80 border-green-200 focus:border-green-400 focus:ring-green-200">
                          <SelectValue placeholder="Select gender" />
                        </SelectTrigger>
                        <SelectContent>
                          <SelectItem value="Male">Male</SelectItem>
                          <SelectItem value="Female">Female</SelectItem>
                        </SelectContent>
                      </Select>
                    </div>
                    <div></div>
                  </div>

                  {/* Row 5: Address */}
                  <div>
                    <Label htmlFor="address" className="text-sm font-medium text-gray-700">Address</Label>
                    <Textarea
                      id="address"
                      value={formData.address}
                      onChange={(e) => setFormData(prev => ({ ...prev, address: e.target.value }))}
                      className="mt-2 bg-white/80 border-green-200 focus:border-green-400 focus:ring-green-200 resize-none"
                      rows={2}
                      placeholder="Enter complete address"
                    />
                  </div>

                  {/* Row 6: Academic Info */}
                  <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
                    <div>
                      <Label htmlFor="gradeLevel" className="text-sm font-medium text-gray-700">Grade Level</Label>
                      <Input
                        id="gradeLevel"
                        value={formData.gradeLevel}
                        onChange={(e) => setFormData(prev => ({ ...prev, gradeLevel: e.target.value }))}
                        className="mt-2 bg-white/80 border-green-200 focus:border-green-400 focus:ring-green-200"
                        required
                      />
                    </div>

                    <div>
                      <Label htmlFor="enrollmentYear" className="text-sm font-medium text-gray-700">Enrollment Year</Label>
                      <Input
                        id="enrollmentYear"
                        type="number"
                        value={formData.enrollmentYear}
                        onChange={(e) => setFormData(prev => ({ ...prev, enrollmentYear: parseInt(e.target.value) || new Date().getFullYear() }))}
                        className="mt-2 bg-white/80 border-green-200 focus:border-green-400 focus:ring-green-200"
                        min="1900"
                        max="2100"
                        required
                      />
                    </div>
                  </div>
                </div>
              </div>
            </div>
          </div>

          {/* Action Buttons */}
          <div className="p-8 border-t border-gray-200/60 bg-gradient-to-r from-gray-50/50 to-green-50/30">
            <div className="flex items-center justify-end gap-4 max-w-7xl mx-auto">
              <Button
                type="button"
                variant="outline"
                onClick={() => setOpen(false)}
                className="border-gray-300 hover:bg-gray-50 px-6 py-2"
              >
                Cancel
              </Button>
              <Button
                type="submit"
                disabled={editStudentMutation.isPending}
                className="bg-gradient-to-r from-green-600 to-emerald-600 hover:from-green-700 hover:to-emerald-700 shadow-lg hover:shadow-xl transition-all duration-300 min-w-[140px] px-6 py-2"
              >
                {editStudentMutation.isPending ? "Updating..." : "Update Student"}
              </Button>
            </div>
          </div>
        </form>
      </SheetContent>
    </Sheet>
  )
} 