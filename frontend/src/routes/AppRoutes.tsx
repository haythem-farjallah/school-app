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

/* -------------------------------------------------------------------------- */
/*  Lazy pages                                                                */
/* -------------------------------------------------------------------------- */
const Login = lazy(() => import("@/pages/Login"));

// Admin pages
const AdminDashboard = lazy(() => import("@/pages/admin/Dashboard"));
const SchoolManagement = lazy(() => import("@/pages/admin/SchoolManagement"));
const Students = lazy(() => import("@/pages/admin/Students"));
const StudentsCreate = lazy(() => import("@/pages/admin/StudentsCreate"));
const StudentsView = lazy(() => import("@/pages/admin/StudentsView"));
const Teachers = lazy(() => import("@/pages/admin/Teachers"));
const TeachersCreate = lazy(() => import("@/pages/admin/TeachersCreate"));
const TeachersView = lazy(() => import("@/pages/admin/TeachersView"));
const Parents = lazy(() => import("@/pages/admin/Parents"));
const ParentsCreate = lazy(() => import("@/pages/admin/ParentsCreate"));
const ParentsView = lazy(() => import("@/pages/admin/ParentsView"));
const Staff = lazy(() => import("@/pages/admin/Staff"));
const StaffsCreate = lazy(() => import("@/pages/admin/StaffsCreate"));
const StaffsView = lazy(() => import("@/pages/admin/StaffsView"));
const AdminCourses = lazy(() => import("@/pages/admin/Courses"));
const CoursesCreate = lazy(() => import("@/pages/admin/CoursesCreate"));
const CoursesView = lazy(() => import("@/pages/admin/CoursesView"));
const Classes = lazy(() => import("@/pages/admin/Classes"));
const ClassesCreate = lazy(() => import("@/pages/admin/ClassesCreate"));
const ClassesView = lazy(() => import("@/pages/admin/ClassesView"));
const LearningResources = lazy(() => import("@/pages/admin/LearningResources"));
const Rooms = lazy(() => import("@/pages/admin/Rooms"));
const RoomsCreate = lazy(() => import("@/pages/admin/RoomsCreate"));
const RoomsView = lazy(() => import("@/pages/admin/RoomsView"));
const AdminGrades = lazy(() => import("@/pages/admin/Grades"));
const Enrollments = lazy(() => import("@/pages/admin/Enrollments"));
const EnrollmentsCreate = lazy(() => import("@/pages/admin/EnrollmentsCreate"));
const EnrollmentsView = lazy(() => import("@/pages/admin/EnrollmentsView"));
const Schedule = lazy(() => import("@/pages/admin/Schedule"));
const Announcements = lazy(() => import("@/pages/admin/Announcements"));
const Settings = lazy(() => import("@/pages/admin/Settings"));

// Teacher pages
const TeacherDashboard = lazy(() => import("@/pages/teacher/Dashboard"));
const MyClasses = lazy(() => import("@/pages/teacher/MyClasses"));
const Assignments = lazy(() => import("@/pages/teacher/Assignments"));
const Grades = lazy(() => import("@/pages/teacher/Grades"));
const TeacherSchedule = lazy(() => import("@/pages/teacher/Schedule"));
const Messages = lazy(() => import("@/pages/teacher/Messages"));
const TeacherAnnouncements = lazy(() => import("@/pages/teacher/Announcements"));

// Student pages
const StudentDashboard = lazy(() => import("@/pages/student/Dashboard"));
const StudentSchedule = lazy(() => import("@/pages/student/Schedule"));
const StudentGrades = lazy(() => import("@/pages/student/Grades"));
const StudentAssignments = lazy(() => import("@/pages/student/Assignments"));
const StudentAnnouncements = lazy(() => import("@/pages/student/Announcements"));

// Parent pages
const ParentDashboard = lazy(() => import("@/pages/parent/Dashboard"));
const MyChildren = lazy(() => import("@/pages/parent/MyChildren"));
const ParentGrades = lazy(() => import("@/pages/parent/Grades"));

// Common pages
const Profile = lazy(() => import("@/pages/Profile"));
const Home = lazy(() => import("@/pages/Home"));

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
          { path: "students/create", element: <Suspense fallback={<Shimmer />}><StudentsCreate /></Suspense> },
          { path: "students/view/:id", element: <Suspense fallback={<Shimmer />}><StudentsView /></Suspense> },
          { path: "teachers", element: <Suspense fallback={<Shimmer />}><Teachers /></Suspense> },
          { path: "teachers/create", element: <Suspense fallback={<Shimmer />}><TeachersCreate /></Suspense> },
          { path: "teachers/view/:id", element: <Suspense fallback={<Shimmer />}><TeachersView /></Suspense> },
          { path: "parents", element: <Suspense fallback={<Shimmer />}><Parents /></Suspense> },
          { path: "parents/create", element: <Suspense fallback={<Shimmer />}><ParentsCreate /></Suspense> },
          { path: "parents/view/:id", element: <Suspense fallback={<Shimmer />}><ParentsView /></Suspense> },
          { path: "staff", element: <Suspense fallback={<Shimmer />}><Staff /></Suspense> },
          { path: "staff/create", element: <Suspense fallback={<Shimmer />}><StaffsCreate /></Suspense> },
          { path: "staff/view/:id", element: <Suspense fallback={<Shimmer />}><StaffsView /></Suspense> },

          { path: "courses", element: <Suspense fallback={<Shimmer />}><AdminCourses /></Suspense> },
          { path: "courses/create", element: <Suspense fallback={<Shimmer />}><CoursesCreate /></Suspense> },
          { path: "courses/view/:id", element: <Suspense fallback={<Shimmer />}><CoursesView /></Suspense> },
          { path: "classes", element: <Suspense fallback={<Shimmer />}><Classes /></Suspense> },
          { path: "classes/create", element: <Suspense fallback={<Shimmer />}><ClassesCreate /></Suspense> },
          { path: "classes/view/:id", element: <Suspense fallback={<Shimmer />}><ClassesView /></Suspense> },
          { path: "learning-resources", element: <Suspense fallback={<Shimmer />}><LearningResources /></Suspense> },
          { path: "rooms", element: <Suspense fallback={<Shimmer />}><Rooms /></Suspense> },
          { path: "rooms/create", element: <Suspense fallback={<Shimmer />}><RoomsCreate /></Suspense> },
          { path: "rooms/view/:id", element: <Suspense fallback={<Shimmer />}><RoomsView /></Suspense> },
          { path: "grades", element: <Suspense fallback={<Shimmer />}><AdminGrades /></Suspense> },
          { path: "enrollments", element: <Suspense fallback={<Shimmer />}><Enrollments /></Suspense> },
          { path: "enrollments/create", element: <Suspense fallback={<Shimmer />}><EnrollmentsCreate /></Suspense> },
          { path: "enrollments/view/:id", element: <Suspense fallback={<Shimmer />}><EnrollmentsView /></Suspense> },
          { path: "schedule", element: <Suspense fallback={<Shimmer />}><Schedule /></Suspense> },
          { path: "announcements", element: <Suspense fallback={<Shimmer />}><Announcements /></Suspense> },
          { path: "settings", element: <Suspense fallback={<Shimmer />}><Settings /></Suspense> },

        ],
      },
      // Teacher Routes
      {
        path: "teacher",
        element: <RoleGuard allowedRoles={["TEACHER"]}><Outlet /></RoleGuard>,
        children: [
          { path: "dashboard", element: <Suspense fallback={<Shimmer />}><TeacherDashboard /></Suspense> },
          { path: "classes", element: <Suspense fallback={<Shimmer />}><MyClasses /></Suspense> },
          { path: "assignments", element: <Suspense fallback={<Shimmer />}><Assignments /></Suspense> },
          { path: "grades", element: <Suspense fallback={<Shimmer />}><Grades /></Suspense> },
          { path: "schedule", element: <Suspense fallback={<Shimmer />}><TeacherSchedule /></Suspense> },
          { path: "messages", element: <Suspense fallback={<Shimmer />}><Messages /></Suspense> },
          { path: "announcements", element: <Suspense fallback={<Shimmer />}><TeacherAnnouncements /></Suspense> },
        ],
      },

      // Student Routes
      {
        path: "student",
        element: <RoleGuard allowedRoles={["STUDENT"]}><Outlet /></RoleGuard>,
        children: [
          { path: "dashboard", element: <Suspense fallback={<Shimmer />}><StudentDashboard /></Suspense> },
          { path: "schedule", element: <Suspense fallback={<Shimmer />}><StudentSchedule /></Suspense> },
          { path: "grades", element: <Suspense fallback={<Shimmer />}><StudentGrades /></Suspense> },
          { path: "assignments", element: <Suspense fallback={<Shimmer />}><StudentAssignments /></Suspense> },
          { path: "announcements", element: <Suspense fallback={<Shimmer />}><StudentAnnouncements /></Suspense> },
        ],
      },

      // Parent Routes
      {
        path: "parent",
        element: <RoleGuard allowedRoles={["PARENT"]}><Outlet /></RoleGuard>,
        children: [
          { path: "dashboard", element: <Suspense fallback={<Shimmer />}><ParentDashboard /></Suspense> },
          { path: "children", element: <Suspense fallback={<Shimmer />}><MyChildren /></Suspense> },
          { path: "grades", element: <Suspense fallback={<Shimmer />}><ParentGrades /></Suspense> },
        ],
      },

      // Common Routes
      { path: "profile", element: <Suspense fallback={<Shimmer />}><Profile /></Suspense> },
      { path: "", element: <Suspense fallback={<Shimmer />}><Home /></Suspense> },
    ],
  },
]);

/* -------------------------------------------------------------------------- */
/*  Router provider                                                           */
/* -------------------------------------------------------------------------- */
export const AppRoutes = () => <RouterProvider router={router} />;
