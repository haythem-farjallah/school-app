import {
  Home,
  School,
  User,
  Users,
  ClipboardList,
  Calendar,
  MessageCircle,
  Megaphone,
  Settings,
  User2,
  GraduationCap,
  BookMarked,
  FileText,
  Bell,
  // Add new icons
  Library,
  DoorOpen,
  BarChart3,
  UserPlus,
  CalendarDays,
  Users2,
  // Enhanced icons for new features
  CheckSquare,
  CalendarCheck,
  Brain,
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
        { icon: BarChart3, label: 'Grades', href: '/admin/grades' },
        { icon: UserPlus, label: 'Enrollments', href: '/admin/enrollments' },
      ],
    },
    {
      title: 'OPERATIONS',
      items: [
        { icon: CalendarDays, label: 'Timetables', href: '/admin/timetable' },
        { icon: Brain, label: 'Smart Timetable', href: '/admin/smart-timetable' },
        { icon: DoorOpen, label: 'Rooms', href: '/admin/rooms' },
        { icon: Calendar, label: 'Schedule', href: '/admin/schedule' },
        { icon: Megaphone, label: 'Announcements', href: '/admin/announcements' },
        { icon: Megaphone, label: 'Enhanced Announcements', href: '/admin/enhanced-announcements' },
        { icon: MessageCircle, label: 'Communication Dashboard', href: '/admin/communication-dashboard' },
        { icon: Settings, label: 'Settings', href: '/admin/settings' },
        { icon: Settings, label: 'Permissions', href: '/admin/permissions', permission: 'PERMISSIONS_MANAGE' },
        
      ],
    },
  ],
  TEACHER: [
    {
      title: 'MENU',
      items: [
        { icon: Home, label: 'Home', href: '/teacher/dashboard' },
        { icon: Calendar, label: 'Schedule', href: '/teacher/schedule' },
        { icon: Users, label: 'My Classes', href: '/teacher/classes' },
        { icon: BarChart3, label: 'Grades', href: '/teacher/grades' },
        { icon: CheckSquare, label: 'Attendance', href: '/teacher/attendance' },
        { icon: Bell, label: 'Announcements', href: '/teacher/announcements' },
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
        { icon: Calendar, label: 'Schedule', href: '/student/schedule' },
        { icon: FileText, label: 'Exams', href: '/student/exams' },
        { icon: FileText, label: 'Results', href: '/student/results' },
        { icon: Calendar, label: 'Events', href: '/student/events' },
        { icon: Bell, label: 'Announcements', href: '/student/announcements' },
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
        { icon: User2, label: 'Children', href: '/parent/children' },
        { icon: GraduationCap, label: 'Results', href: '/parent/grades' },
        { icon: Calendar, label: 'Schedule', href: '/parent/schedule' },
        { icon: CheckSquare, label: 'Attendance', href: '/parent/attendance' },
        { icon: Bell, label: 'Announcements', href: '/parent/announcements' },
        { icon: CalendarCheck, label: 'Meetings', href: '/parent/meetings' },
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
        { icon: BarChart3, label: 'Grade Management', href: '/staff/grades' },
        { icon: CheckSquare, label: 'Attendance Management', href: '/staff/attendance' },
        { icon: Calendar, label: 'Schedule Management', href: '/staff/schedule' },
        { icon: DoorOpen, label: 'Room Management', href: '/staff/rooms' },

      ],
    },
    {
      title: 'COMMUNICATION',
      items: [
        { icon: Bell, label: 'Announcements', href: '/staff/announcements' },

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