import { lazy, Suspense } from "react";
import {
  createBrowserRouter,
  RouterProvider,
  Outlet,
} from "react-router-dom";
import { Layout } from "@/components/Layout/Layout";
import Shimmer from "@/components/Shimmer/Shimmer";
import ErrorRouteElement from "@/components/Shared/ErrorRouteElement";
import { RoleGuard } from "@/components/guards/RoleGuard";
import { PermissionGuard } from "@/components/guards/PermissionGuard";

/* -------------------------------------------------------------------------- */
/*  Lazy pages                                                                */
/* -------------------------------------------------------------------------- */
// NOTE: Some advanced pages are temporarily commented out due to import issues
// These will be uncommented once module resolution issues are resolved
const Login = lazy(() => import("@/pages/Login"));
const ChangePassword = lazy(() => import("@/pages/ChangePassword"));
const ForgotPassword = lazy(() => import("@/pages/ForgotPassword"));
const ResetPassword = lazy(() => import("@/pages/ResetPassword"));

// Admin pages
const AdminDashboard = lazy(() => import("@/pages/admin/Dashboard"));
const SchoolManagement = lazy(() => import("@/pages/admin/SchoolManagement"));
const Students = lazy(() => import("@/pages/admin/Students"));
const StudentCreate = lazy(() => import("@/pages/admin/StudentCreate"));
const StudentsView = lazy(() => import("@/pages/admin/StudentsView"));
const Teachers = lazy(() => import("@/pages/admin/Teachers"));
const TeacherCreate = lazy(() => import("@/pages/admin/TeacherCreate"));
const TeachersView = lazy(() => import("@/pages/admin/TeachersView"));
const Parents = lazy(() => import("@/pages/admin/Parents"));
const ParentCreate = lazy(() => import("@/pages/admin/ParentCreate"));
const ParentsView = lazy(() => import("@/pages/admin/ParentsView"));
const Staff = lazy(() => import("@/pages/admin/Staff"));
const StaffCreate = lazy(() => import("@/pages/admin/StaffCreate"));
const StaffsView = lazy(() => import("@/pages/admin/StaffsView"));
const AdminCourses = lazy(() => import("@/pages/admin/Courses"));
const CourseCreate = lazy(() => import("@/pages/admin/CourseCreate"));
const CoursesView = lazy(() => import("@/pages/admin/CoursesView"));
const Classes = lazy(() => import("@/pages/admin/Classes"));
const ClassCreate = lazy(() => import("@/pages/admin/ClassCreate"));
const ClassesView = lazy(() => import("@/pages/admin/ClassesView"));
const LearningResources = lazy(() => import("@/pages/admin/LearningResources"));
const Rooms = lazy(() => import("@/pages/admin/Rooms"));
const RoomCreate = lazy(() => import("@/pages/admin/RoomCreate"));
const RoomsView = lazy(() => import("@/pages/admin/RoomsView"));
const AdminGrades = lazy(() => import("@/pages/admin/Grades"));
const Enrollments = lazy(() => import("@/pages/admin/Enrollments"));
const EnrollmentCreate = lazy(() => import("@/pages/admin/EnrollmentCreate"));
const EnrollmentsView = lazy(() => import("@/pages/admin/EnrollmentsView"));
const Schedule = lazy(() => import("@/pages/admin/Schedule"));
const Announcements = lazy(() => import("@/pages/admin/Announcements"));
const Settings = lazy(() => import("@/pages/admin/Settings"));
const Permissions = lazy(() => import("@/pages/admin/Permissions"));
const TimetablePage = lazy(() => import("@/pages/admin/Timetable"));
const TeachingAssignments = lazy(() => import("@/pages/admin/TeachingAssignments"));
const TeachingAssignmentsLinking = lazy(() => import("@/pages/admin/TeachingAssignmentsLinking"));
const GradeReview = lazy(() => import("@/pages/admin/GradeReview"));
const TeacherAttendanceManagement = lazy(() => import("@/pages/admin/TeacherAttendance"));
const TeacherSchedulePage = lazy(() => import("@/pages/admin/TeacherSchedule"));
const SmartTimetable = lazy(() => import("@/pages/admin/SmartTimetable"));
const CommunicationDashboard = lazy(() => import("@/pages/admin/CommunicationDashboard"));
const EnhancedAnnouncements = lazy(() => import("@/pages/admin/EnhancedAnnouncements"));

// Staff pages
const StaffDashboard = lazy(() => import("@/pages/staff/Dashboard"));
const StaffOperations = lazy(() => import("@/pages/staff/Operations"));
const StaffStudents = lazy(() => import("@/pages/staff/Students"));
const StaffEnrollments = lazy(() => import("@/pages/staff/Enrollments"));
const StaffSchedule = lazy(() => import("@/pages/staff/Schedule"));
const StaffRooms = lazy(() => import("@/pages/staff/Rooms"));
const StaffClasses = lazy(() => import("@/pages/staff/Classes"));
const StaffReports = lazy(() => import("@/pages/staff/Reports"));
const StaffAnnouncements = lazy(() => import("@/pages/staff/Announcements"));
const StaffMessages = lazy(() => import("@/pages/staff/Messages"));

// Teacher pages
const TeacherDashboard = lazy(() => import("@/pages/teacher/Dashboard"));
const MyClasses = lazy(() => import("@/pages/teacher/MyClasses"));
const TeacherMyTimetable = lazy(() => import("@/pages/teacher/MyTimetable"));

const Assignments = lazy(() => import("@/pages/teacher/Assignments"));
const Grades = lazy(() => import("@/pages/teacher/Grades"));
const TeacherSchedule = lazy(() => import("@/pages/teacher/Schedule"));
const Messages = lazy(() => import("@/pages/teacher/Messages"));
const TeacherAnnouncements = lazy(() => import("@/pages/teacher/Announcements"));
// Teacher additional pages (temporarily commented out for troubleshooting)
const TeacherAttendance = lazy(() => import("@/pages/teacher/Attendance"));
// const TeacherTeachingAssignments = lazy(() => import("@/pages/teacher/TeachingAssignments"));
const TeacherGradeWorkflow = lazy(() => import("@/pages/teacher/GradeWorkflow"));
// const TeacherParentContact = lazy(() => import("@/pages/teacher/ParentContact"));

// Student pages
const StudentDashboard = lazy(() => import("@/pages/student/Dashboard"));
const StudentSchedule = lazy(() => import("@/pages/student/Schedule"));
const StudentMyTimetable = lazy(() => import("@/pages/student/MyTimetable"));
const StudentGrades = lazy(() => import("@/pages/student/Grades"));
const StudentAssignments = lazy(() => import("@/pages/student/Assignments"));
const StudentAnnouncements = lazy(() => import("@/pages/student/Announcements"));
const StudentExams = lazy(() => import("@/pages/student/Exams"));
const StudentResults = lazy(() => import("@/pages/student/Results"));
const StudentEvents = lazy(() => import("@/pages/student/Events"));
// Student additional pages (temporarily commented out for troubleshooting)
const StudentCourses = lazy(() => import("@/pages/student/Courses"));
// const StudentProgress = lazy(() => import("@/pages/student/Progress"));
// const StudentAttendance = lazy(() => import("@/pages/student/Attendance"));
// const StudentAchievements = lazy(() => import("@/pages/student/Achievements"));
const StudentMessages = lazy(() => import("@/pages/student/Messages"));

// Parent pages
const ParentDashboard = lazy(() => import("@/pages/parent/Dashboard"));
const MyChildren = lazy(() => import("@/pages/parent/MyChildren"));
const ParentGrades = lazy(() => import("@/pages/parent/Grades"));
const ParentSchedule = lazy(() => import("@/pages/parent/Schedule"));
const ParentAssignments = lazy(() => import("@/pages/parent/Assignments"));
const ParentAttendance = lazy(() => import("@/pages/parent/Attendance"));
const ParentReports = lazy(() => import("@/pages/parent/Reports"));
const ParentMessages = lazy(() => import("@/pages/parent/Messages"));
const ParentAnnouncements = lazy(() => import("@/pages/parent/Announcements"));
const ParentContact = lazy(() => import("@/pages/parent/Contact"));
const ParentMeetings = lazy(() => import("@/pages/parent/Meetings"));
const ParentCalls = lazy(() => import("@/pages/parent/Calls"));
const ParentFamilyProgress = lazy(() => import("@/pages/parent/FamilyProgress"));

// Staff pages (temporarily commented out for troubleshooting)  
// const StaffOperations = lazy(() => import("@/pages/staff/Operations"));
// const StaffStudents = lazy(() => import("@/pages/staff/Students"));
// const StaffEnrollments = lazy(() => import("@/pages/staff/Enrollments"));
// const StaffSchedule = lazy(() => import("@/pages/staff/Schedule"));
// const StaffRooms = lazy(() => import("@/pages/staff/Rooms"));
// const StaffReports = lazy(() => import("@/pages/staff/Reports"));
// const StaffAnnouncements = lazy(() => import("@/pages/staff/Announcements"));
// const StaffMessages = lazy(() => import("@/pages/staff/Messages"));

// Common pages
const SettingsPage = lazy(() => import("@/pages/Profile"));
const Home = lazy(() => import("@/pages/Home"));
const LearningSpace = lazy(() => import("@/pages/LearningSpace"));

// Learning Resources  
const TeacherResourceUpload = lazy(() => import("@/features/learning-resources/components/TeacherResourceUpload"));

/* -------------------------------------------------------------------------- */
/*  Router definition                                                         */
/* -------------------------------------------------------------------------- */
const router = createBrowserRouter([
  {
    path: "/login",
    element: (
      <Suspense fallback={<Shimmer />}>
        <Login />
      </Suspense>
    ),
  },
  {
    path: "/change-password",
    element: (
      <Suspense fallback={<Shimmer />}>
        <ChangePassword />
      </Suspense>
    ),
  },
  {
    path: "/forgot-password",
    element: (
      <Suspense fallback={<Shimmer />}>
        <ForgotPassword />
      </Suspense>
    ),
  },
  {
    path: "/reset-password",
    element: (
      <Suspense fallback={<Shimmer />}>
        <ResetPassword />
      </Suspense>
    ),
  },
  {
    path: "/",
    element: <Layout />,
    errorElement: <ErrorRouteElement />,
    children: [
      // Admin Routes
      {
        path: "admin",
        element: <RoleGuard allowedRoles={["ADMIN"]}><Outlet /></RoleGuard>,
        children: [
          { path: "dashboard", element: <Suspense fallback={<Shimmer />}><AdminDashboard /></Suspense> },
          { path: "school", element: <Suspense fallback={<Shimmer />}><SchoolManagement /></Suspense> },
          { path: "students", element: <Suspense fallback={<Shimmer />}><Students /></Suspense> },
          { path: "students/create", element: <Suspense fallback={<Shimmer />}><StudentCreate /></Suspense> },
          { path: "students/view/:id", element: <Suspense fallback={<Shimmer />}><StudentsView /></Suspense> },
          { path: "teachers", element: <Suspense fallback={<Shimmer />}><Teachers /></Suspense> },
          { path: "teachers/create", element: <Suspense fallback={<Shimmer />}><TeacherCreate /></Suspense> },
          { path: "teachers/view/:id", element: <Suspense fallback={<Shimmer />}><TeachersView /></Suspense> },
          { path: "parents", element: <Suspense fallback={<Shimmer />}><Parents /></Suspense> },
          { path: "parents/create", element: <Suspense fallback={<Shimmer />}><ParentCreate /></Suspense> },
          { path: "parents/view/:id", element: <Suspense fallback={<Shimmer />}><ParentsView /></Suspense> },
          { path: "staff", element: <Suspense fallback={<Shimmer />}><Staff /></Suspense> },
          { path: "staff/create", element: <Suspense fallback={<Shimmer />}><StaffCreate /></Suspense> },
          { path: "staff/view/:id", element: <Suspense fallback={<Shimmer />}><StaffsView /></Suspense> },

          { path: "courses", element: <Suspense fallback={<Shimmer />}><AdminCourses /></Suspense> },
          { path: "courses/create", element: <Suspense fallback={<Shimmer />}><CourseCreate /></Suspense> },
          { path: "courses/view/:id", element: <Suspense fallback={<Shimmer />}><CoursesView /></Suspense> },
          { path: "classes", element: <Suspense fallback={<Shimmer />}><Classes /></Suspense> },
          { path: "classes/create", element: <Suspense fallback={<Shimmer />}><ClassCreate /></Suspense> },
          { path: "classes/view/:id", element: <Suspense fallback={<Shimmer />}><ClassesView /></Suspense> },
          { path: "learning-resources", element: <Suspense fallback={<Shimmer />}><LearningResources /></Suspense> },
          { path: "rooms", element: <Suspense fallback={<Shimmer />}><Rooms /></Suspense> },
          { path: "rooms/create", element: <Suspense fallback={<Shimmer />}><RoomCreate /></Suspense> },
          { path: "rooms/view/:id", element: <Suspense fallback={<Shimmer />}><RoomsView /></Suspense> },
          { path: "grades", element: <Suspense fallback={<Shimmer />}><AdminGrades /></Suspense> },
          { path: "enrollments", element: <Suspense fallback={<Shimmer />}><Enrollments /></Suspense> },
          { path: "enrollments/create", element: <Suspense fallback={<Shimmer />}><EnrollmentCreate /></Suspense> },
          { path: "enrollments/view/:id", element: <Suspense fallback={<Shimmer />}><EnrollmentsView /></Suspense> },
          { path: "schedule", element: <Suspense fallback={<Shimmer />}><Schedule /></Suspense> },
          { path: "announcements", element: <Suspense fallback={<Shimmer />}><Announcements /></Suspense> },
          { path: "settings", element: <Suspense fallback={<Shimmer />}><Settings /></Suspense> },
          { path: "permissions", element: <Suspense fallback={<Shimmer />}><PermissionGuard required="PERMISSIONS_MANAGE"><Permissions /></PermissionGuard></Suspense> },
          { path: "timetable", element: <Suspense fallback={<Shimmer />}><TimetablePage /></Suspense> },
          { path: "timetable/:classId", element: <Suspense fallback={<Shimmer />}><TimetablePage /></Suspense> },
          { path: "teacher-schedules", element: <Suspense fallback={<Shimmer />}><TeacherSchedulePage /></Suspense> },
          { path: "smart-timetable", element: <Suspense fallback={<Shimmer />}><SmartTimetable /></Suspense> },
          { path: "communication-dashboard", element: <Suspense fallback={<Shimmer />}><CommunicationDashboard /></Suspense> },
          { path: "enhanced-announcements", element: <Suspense fallback={<Shimmer />}><EnhancedAnnouncements /></Suspense> },
          { path: "teaching-assignments", element: <Suspense fallback={<Shimmer />}><TeachingAssignments /></Suspense> },
          { path: "teaching-assignments/linking", element: <Suspense fallback={<Shimmer />}><TeachingAssignmentsLinking /></Suspense> },
          { path: "grade-review", element: <Suspense fallback={<Shimmer />}><GradeReview /></Suspense> },
          { path: "teacher-attendance", element: <Suspense fallback={<Shimmer />}><TeacherAttendanceManagement /></Suspense> },
          { path: "profile", element: <Suspense fallback={<Shimmer />}><SettingsPage /></Suspense> },

        ],
      },
      // Teacher Routes
      {
        path: "teacher",
        element: <RoleGuard allowedRoles={["TEACHER"]}><Outlet /></RoleGuard>,
        children: [
          { path: "dashboard", element: <Suspense fallback={<Shimmer />}><TeacherDashboard /></Suspense> },
          { path: "classes", element: <Suspense fallback={<Shimmer />}><MyClasses /></Suspense> },
          { path: "my-timetable", element: <Suspense fallback={<Shimmer />}><TeacherMyTimetable /></Suspense> },
          // { path: "teaching-assignments", element: <Suspense fallback={<Shimmer />}><TeacherTeachingAssignments /></Suspense> },
          { path: "assignments", element: <Suspense fallback={<Shimmer />}><Assignments /></Suspense> },
          { path: "grade-workflow", element: <Suspense fallback={<Shimmer />}><TeacherGradeWorkflow /></Suspense> },
          { path: "grades", element: <Suspense fallback={<Shimmer />}><Grades /></Suspense> },
          { path: "attendance", element: <Suspense fallback={<Shimmer />}><TeacherAttendance /></Suspense> },
          { path: "schedule", element: <Suspense fallback={<Shimmer />}><TeacherSchedule /></Suspense> },
          { path: "messages", element: <Suspense fallback={<Shimmer />}><Messages /></Suspense> },
          { path: "announcements", element: <Suspense fallback={<Shimmer />}><TeacherAnnouncements /></Suspense> },
          { path: "upload-resource", element: <Suspense fallback={<Shimmer />}><TeacherResourceUpload /></Suspense> },
          // { path: "parent-contact", element: <Suspense fallback={<Shimmer />}><TeacherParentContact /></Suspense> },
          { path: "profile", element: <Suspense fallback={<Shimmer />}><SettingsPage /></Suspense> },
          { path: "settings", element: <Suspense fallback={<Shimmer />}><SettingsPage /></Suspense> },
        ],
      },

      // Student Routes
      {
        path: "student",
        element: <RoleGuard allowedRoles={["STUDENT"]}><Outlet /></RoleGuard>,
        children: [
          { path: "dashboard", element: <Suspense fallback={<Shimmer />}><StudentDashboard /></Suspense> },
          { path: "courses", element: <Suspense fallback={<Shimmer />}><StudentCourses /></Suspense> },
          { path: "schedule", element: <Suspense fallback={<Shimmer />}><StudentSchedule /></Suspense> },
          { path: "my-timetable", element: <Suspense fallback={<Shimmer />}><StudentMyTimetable /></Suspense> },
          { path: "exams", element: <Suspense fallback={<Shimmer />}><StudentExams /></Suspense> },
          { path: "results", element: <Suspense fallback={<Shimmer />}><StudentResults /></Suspense> },
          { path: "events", element: <Suspense fallback={<Shimmer />}><StudentEvents /></Suspense> },
          { path: "assignments", element: <Suspense fallback={<Shimmer />}><StudentAssignments /></Suspense> },
          { path: "grades", element: <Suspense fallback={<Shimmer />}><StudentGrades /></Suspense> },
          // { path: "progress", element: <Suspense fallback={<Shimmer />}><StudentProgress /></Suspense> },
          // { path: "attendance", element: <Suspense fallback={<Shimmer />}><StudentAttendance /></Suspense> },
          // { path: "achievements", element: <Suspense fallback={<Shimmer />}><StudentAchievements /></Suspense> },
          { path: "messages", element: <Suspense fallback={<Shimmer />}><StudentMessages /></Suspense> },
          { path: "announcements", element: <Suspense fallback={<Shimmer />}><StudentAnnouncements /></Suspense> },
          { path: "profile", element: <Suspense fallback={<Shimmer />}><SettingsPage /></Suspense> },
          { path: "settings", element: <Suspense fallback={<Shimmer />}><SettingsPage /></Suspense> },
        ],
      },

      // Parent Routes
      {
        path: "parent",
        element: <RoleGuard allowedRoles={["PARENT"]}><Outlet /></RoleGuard>,
        children: [
          { path: "dashboard", element: <Suspense fallback={<Shimmer />}><ParentDashboard /></Suspense> },
          { path: "children", element: <Suspense fallback={<Shimmer />}><MyChildren /></Suspense> },
          { path: "family-progress", element: <Suspense fallback={<Shimmer />}><ParentFamilyProgress /></Suspense> },
          { path: "grades", element: <Suspense fallback={<Shimmer />}><ParentGrades /></Suspense> },
          { path: "schedule", element: <Suspense fallback={<Shimmer />}><ParentSchedule /></Suspense> },
          { path: "assignments", element: <Suspense fallback={<Shimmer />}><ParentAssignments /></Suspense> },
          { path: "attendance", element: <Suspense fallback={<Shimmer />}><ParentAttendance /></Suspense> },
          { path: "reports", element: <Suspense fallback={<Shimmer />}><ParentReports /></Suspense> },
          { path: "messages", element: <Suspense fallback={<Shimmer />}><ParentMessages /></Suspense> },
          { path: "announcements", element: <Suspense fallback={<Shimmer />}><ParentAnnouncements /></Suspense> },
          { path: "contact", element: <Suspense fallback={<Shimmer />}><ParentContact /></Suspense> },
          { path: "meetings", element: <Suspense fallback={<Shimmer />}><ParentMeetings /></Suspense> },
          { path: "calls", element: <Suspense fallback={<Shimmer />}><ParentCalls /></Suspense> },
          { path: "profile", element: <Suspense fallback={<Shimmer />}><SettingsPage /></Suspense> },
          { path: "settings", element: <Suspense fallback={<Shimmer />}><SettingsPage /></Suspense> },
        ],
      },

      // Staff Routes
      {
        path: "staff",
        element: <RoleGuard allowedRoles={["STAFF"]}><Outlet /></RoleGuard>,
        children: [
          { path: "dashboard", element: <Suspense fallback={<Shimmer />}><StaffDashboard /></Suspense> },
          { path: "operations", element: <Suspense fallback={<Shimmer />}><StaffOperations /></Suspense> },
          { path: "students", element: <Suspense fallback={<Shimmer />}><StaffStudents /></Suspense> },
          { path: "students/create", element: <Suspense fallback={<Shimmer />}><StudentCreate /></Suspense> },
          { path: "students/view/:id", element: <Suspense fallback={<Shimmer />}><StudentsView /></Suspense> },
          { path: "teachers", element: <Suspense fallback={<Shimmer />}><Teachers /></Suspense> },
          { path: "teachers/create", element: <Suspense fallback={<Shimmer />}><TeacherCreate /></Suspense> },
          { path: "teachers/view/:id", element: <Suspense fallback={<Shimmer />}><TeachersView /></Suspense> },
          { path: "parents", element: <Suspense fallback={<Shimmer />}><Parents /></Suspense> },
          { path: "parents/create", element: <Suspense fallback={<Shimmer />}><ParentCreate /></Suspense> },
          { path: "parents/view/:id", element: <Suspense fallback={<Shimmer />}><ParentsView /></Suspense> },
          { path: "classes", element: <Suspense fallback={<Shimmer />}><StaffClasses /></Suspense> },
          { path: "classes/create", element: <Suspense fallback={<Shimmer />}><ClassCreate /></Suspense> },
          { path: "classes/view/:id", element: <Suspense fallback={<Shimmer />}><ClassesView /></Suspense> },
          { path: "enrollments", element: <Suspense fallback={<Shimmer />}><StaffEnrollments /></Suspense> },
          { path: "grades", element: <Suspense fallback={<Shimmer />}><AdminGrades /></Suspense> },
          { path: "attendance", element: <Suspense fallback={<Shimmer />}><TeacherAttendanceManagement /></Suspense> },
          { path: "schedule", element: <Suspense fallback={<Shimmer />}><StaffSchedule /></Suspense> },
          { path: "rooms", element: <Suspense fallback={<Shimmer />}><StaffRooms /></Suspense> },
          { path: "rooms/create", element: <Suspense fallback={<Shimmer />}><RoomCreate /></Suspense> },
          { path: "rooms/view/:id", element: <Suspense fallback={<Shimmer />}><RoomsView /></Suspense> },
          { path: "reports", element: <Suspense fallback={<Shimmer />}><StaffReports /></Suspense> },
          { path: "announcements", element: <Suspense fallback={<Shimmer />}><StaffAnnouncements /></Suspense> },
          { path: "messages", element: <Suspense fallback={<Shimmer />}><StaffMessages /></Suspense> },
          { path: "profile", element: <Suspense fallback={<Shimmer />}><SettingsPage /></Suspense> },
          { path: "settings", element: <Suspense fallback={<Shimmer />}><SettingsPage /></Suspense> },
        ],
      },

      // Common Routes
      { path: "profile", element: <Suspense fallback={<Shimmer />}><SettingsPage /></Suspense> },
      { path: "learning-space", element: <Suspense fallback={<Shimmer />}><LearningSpace /></Suspense> },
      { path: "", element: <Suspense fallback={<Shimmer />}><Home /></Suspense> },
    ],
  },
]);

/* -------------------------------------------------------------------------- */
/*  Router provider                                                           */
/* -------------------------------------------------------------------------- */
export const AppRoutes = () => <RouterProvider router={router} />;
