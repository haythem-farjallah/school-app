import {
  Home,
  School,
  User,
  Users,
  ClipboardList,
  Calendar,
  Settings,
  User2,
  GraduationCap,
  BookMarked,
  FileText,
  // Add new icons
  Library,
  DoorOpen,
  UserPlus,
  CalendarDays,
  Users2,
  // Enhanced icons for new features
  CheckSquare,
  CalendarCheck,
  Award,
  LogOut,
} from 'lucide-react';

export type MenuItem = {
  icon: React.ComponentType<{ className?: string }>;
  label: string;
  href: string;
  permission?: string;
};

export type MenuSection = {
  title: string;
  items: MenuItem[];
};

export const menuConfig: Record<string, MenuSection[]> = {
  ADMIN: [
    {
      title: 'DASHBOARD',
      items: [
        { icon: Home, label: 'Overview', href: '/admin/dashboard' },
        { icon: School, label: 'School Management', href: '/admin/school' },
      ],
    },
    {
      title: 'ACADEMIC',
      items: [
        { icon: Users, label: 'Students', href: '/admin/students' },
        { icon: User, label: 'Teachers', href: '/admin/teachers' },
        { icon: User2, label: 'Parents', href: '/admin/parents' },
        { icon: Users2, label: 'Staff', href: '/admin/staff' },
        { icon: BookMarked, label: 'Courses', href: '/admin/courses' },
        { icon: ClipboardList, label: 'Classes', href: '/admin/classes' },
        { icon: Library, label: 'Learning Resources', href: '/admin/learning-resources' },
        { icon: UserPlus, label: 'Enrollments', href: '/admin/enrollments' },
      ],
    },
    {
      title: 'OPERATIONS',
      items: [
        { icon: CalendarDays, label: 'Timetables', href: '/admin/timetable' },
        { icon: DoorOpen, label: 'Rooms', href: '/admin/rooms' },
        { icon: Settings, label: 'Settings', href: '/admin/profile' },
        
      ],
    },
  ],
  TEACHER: [
    {
      title: 'MENU',
      items: [
        { icon: Home, label: 'Home', href: '/teacher/dashboard' },
        { icon: Calendar, label: 'My Schedule', href: '/teacher/my-timetable' },
        { icon: CheckSquare, label: 'Attendance', href: '/teacher/attendance' },
      ],
    },
    {
      title: 'OTHER',
      items: [
        { icon: User, label: 'Settings', href: '/teacher/profile' },
        { icon: LogOut, label: 'Logout', href: '/logout' },
      ],
    },
  ],
  STUDENT: [
    {
      title: 'MENU',
      items: [
        { icon: Home, label: 'Home', href: '/student/dashboard' },
        { icon: CheckSquare, label: 'Attendance', href: '/student/attendance' },
        { icon: BookMarked, label: 'Learning Space', href: '/learning-space' },
      ],
    },
    {
      title: 'OTHER',
      items: [
        { icon: User, label: 'Settings', href: '/student/profile' },
        { icon: LogOut, label: 'Logout', href: '/logout' },
      ],
    },
  ],
  PARENT: [
    {
      title: 'MENU',
      items: [
        { icon: Home, label: 'Home', href: '/parent/dashboard' },
        { icon: Calendar, label: 'Timetables', href: '/parent/schedule' },
        { icon: CheckSquare, label: 'Attendance', href: '/parent/attendance' },
      ],
    },
    {
      title: 'OTHER',
      items: [
        { icon: User, label: 'Settings', href: '/parent/profile' },
        { icon: LogOut, label: 'Logout', href: '/logout' },
      ],
    },
  ],
  STAFF: [
    {
      title: 'DASHBOARD',
      items: [
        { icon: Home, label: 'Overview', href: '/staff/dashboard' },

      ],
    },
    {
      title: 'MANAGEMENT',
      items: [
        { icon: Users, label: 'Student Management', href: '/staff/students' },
        { icon: User, label: 'Teacher Management', href: '/staff/teachers' },
        { icon: User2, label: 'Parent Management', href: '/staff/parents' },
        { icon: ClipboardList, label: 'Class Management', href: '/staff/classes' },
        { icon: UserPlus, label: 'Enrollments', href: '/staff/enrollments' },
        { icon: CheckSquare, label: 'Attendance Management', href: '/staff/attendance' },
        { icon: Calendar, label: 'Schedule Management', href: '/staff/schedule' },
        { icon: DoorOpen, label: 'Room Management', href: '/staff/rooms' },

      ],
    },
    {
      title: 'ACCOUNT',
      items: [
        { icon: User, label: 'Settings', href: '/staff/profile' },
      ],
    },
  ],
}; 