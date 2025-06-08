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
const Teachers = lazy(() => import("@/pages/admin/Teachers"));
const Parents = lazy(() => import("@/pages/admin/Parents"));
const Subjects = lazy(() => import("@/pages/admin/Subjects"));
const AdminCourses = lazy(() => import("@/pages/admin/Courses"));
const Classes = lazy(() => import("@/pages/admin/Classes"));
const Schedule = lazy(() => import("@/pages/admin/Schedule"));
const Announcements = lazy(() => import("@/pages/admin/Announcements"));
const Settings = lazy(() => import("@/pages/admin/Settings"));
const AdminLevels = lazy(() => import("@/pages/admin/Levels"));

// Teacher pages
const TeacherDashboard = lazy(() => import("@/pages/teacher/Dashboard"));
const MyClasses = lazy(() => import("@/pages/teacher/MyClasses"));
const Assignments = lazy(() => import("@/pages/teacher/Assignments"));
const Grades = lazy(() => import("@/pages/teacher/Grades"));
const TeacherSchedule = lazy(() => import("@/pages/teacher/Schedule"));
const Messages = lazy(() => import("@/pages/teacher/Messages"));
const TeacherAnnouncements = lazy(() => import("@/pages/teacher/Announcements"));
const TeacherLevels = lazy(() => import("@/pages/teacher/Levels"));

// Student pages
const StudentDashboard = lazy(() => import("@/pages/student/Dashboard"));
const MyCourses = lazy(() => import("@/pages/student/MyCourses"));
const StudentAssignments = lazy(() => import("@/pages/student/Assignments"));
const StudentGrades = lazy(() => import("@/pages/student/Grades"));
const StudentSchedule = lazy(() => import("@/pages/student/Schedule"));
const StudentMessages = lazy(() => import("@/pages/student/Messages"));
const StudentAnnouncements = lazy(() => import("@/pages/student/Announcements"));

// Parent pages
const ParentDashboard = lazy(() => import("@/pages/parent/Dashboard"));
const MyChildren = lazy(() => import("@/pages/parent/MyChildren"));
const ParentGrades = lazy(() => import("@/pages/parent/Grades"));

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
          { path: "teachers", element: <Suspense fallback={<Shimmer />}><Teachers /></Suspense> },
          { path: "parents", element: <Suspense fallback={<Shimmer />}><Parents /></Suspense> },
          { path: "subjects", element: <Suspense fallback={<Shimmer />}><Subjects /></Suspense> },
          { path: "courses", element: <Suspense fallback={<Shimmer />}><AdminCourses /></Suspense> },
          { path: "classes", element: <Suspense fallback={<Shimmer />}><Classes /></Suspense> },
          { path: "schedule", element: <Suspense fallback={<Shimmer />}><Schedule /></Suspense> },
          { path: "announcements", element: <Suspense fallback={<Shimmer />}><Announcements /></Suspense> },
          { path: "settings", element: <Suspense fallback={<Shimmer />}><Settings /></Suspense> },
          { path: "levels", element: <Suspense fallback={<Shimmer />}><AdminLevels /></Suspense> },
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
          { path: "levels", element: <Suspense fallback={<Shimmer />}><TeacherLevels /></Suspense> },
        ],
      },
      // Student Routes
      {
        path: "student",
        element: <RoleGuard allowedRoles={["STUDENT"]}><Outlet /></RoleGuard>,
        children: [
          { path: "dashboard", element: <Suspense fallback={<Shimmer />}><StudentDashboard /></Suspense> },
          { path: "courses", element: <Suspense fallback={<Shimmer />}><MyCourses /></Suspense> },
          { path: "assignments", element: <Suspense fallback={<Shimmer />}><StudentAssignments /></Suspense> },
          { path: "grades", element: <Suspense fallback={<Shimmer />}><StudentGrades /></Suspense> },
          { path: "schedule", element: <Suspense fallback={<Shimmer />}><StudentSchedule /></Suspense> },
          { path: "messages", element: <Suspense fallback={<Shimmer />}><StudentMessages /></Suspense> },
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
    ],
  },
]);

/* -------------------------------------------------------------------------- */
/*  Router provider                                                           */
/* -------------------------------------------------------------------------- */
export const AppRoutes = () => <RouterProvider router={router} />;
