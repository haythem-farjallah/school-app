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

/**
 * A menu entry either navigates to `href` or performs an action (only logout today).
 * `label` and section `title` are translation keys (locales/<lng>/translation.json).
 */
export type MenuItem = {
  icon: React.ComponentType<{ className?: string }>;
  label: string;
  permission?: string;
} & ({ href: string } | { action: 'logout' });

export type MenuSection = {
  title: string;
  items: MenuItem[];
};

/** The role's menu, without items that need a permission the user does not have. */
export function getMenuSections(role: string | undefined, permissions: string[]): MenuSection[] {
  return (menuConfig[role ?? ''] ?? []).map((section) => ({
    ...section,
    items: section.items.filter((item) => !item.permission || permissions.includes(item.permission)),
  }));
}

export const menuConfig: Record<string, MenuSection[]> = {
  ADMIN: [
    {
      title: 'navigation.sections.dashboard',
      items: [
        { icon: Home, label: 'navigation.overview', href: '/admin/dashboard' },
        { icon: School, label: 'navigation.schoolManagement', href: '/admin/school' },
      ],
    },
    {
      title: 'navigation.sections.academic',
      items: [
        { icon: Users, label: 'navigation.students', href: '/admin/students' },
        { icon: User, label: 'navigation.teachers', href: '/admin/teachers' },
        { icon: User2, label: 'navigation.parents', href: '/admin/parents' },
        { icon: Users2, label: 'navigation.staff', href: '/admin/staff' },
        { icon: BookMarked, label: 'navigation.courses', href: '/admin/courses' },
        { icon: ClipboardList, label: 'navigation.classes', href: '/admin/classes' },
        { icon: Library, label: 'navigation.learningResources', href: '/admin/learning-resources' },
        { icon: BarChart3, label: 'navigation.grades', href: '/admin/grades' },
        { icon: UserPlus, label: 'navigation.enrollments', href: '/admin/enrollments' },
      ],
    },
    {
      title: 'navigation.sections.operations',
      items: [
        { icon: CalendarDays, label: 'navigation.timetables', href: '/admin/timetable' },
        { icon: Brain, label: 'navigation.smartTimetable', href: '/admin/smart-timetable' },
        { icon: DoorOpen, label: 'navigation.rooms', href: '/admin/rooms' },
        { icon: Calendar, label: 'navigation.schedule', href: '/admin/schedule' },
        { icon: Megaphone, label: 'navigation.announcements', href: '/admin/announcements' },
        { icon: Megaphone, label: 'navigation.enhancedAnnouncements', href: '/admin/enhanced-announcements' },
        { icon: MessageCircle, label: 'navigation.communicationDashboard', href: '/admin/communication-dashboard' },
        { icon: Settings, label: 'navigation.settings', href: '/admin/settings' },
        { icon: Settings, label: 'navigation.permissions', href: '/admin/permissions', permission: 'PERMISSIONS_MANAGE' },
        
      ],
    },
  ],
  TEACHER: [
    {
      title: 'navigation.sections.menu',
      items: [
        { icon: Home, label: 'navigation.home', href: '/teacher/dashboard' },
        { icon: Calendar, label: 'navigation.schedule', href: '/teacher/schedule' },
        { icon: Users, label: 'navigation.myClasses', href: '/teacher/classes' },
        { icon: BarChart3, label: 'navigation.grades', href: '/teacher/grades' },
        { icon: CheckSquare, label: 'navigation.attendance', href: '/teacher/attendance' },
        { icon: Bell, label: 'navigation.announcements', href: '/teacher/announcements' },
      ],
    },
    {
      title: 'navigation.sections.other',
      items: [
        { icon: User, label: 'navigation.settings', href: '/teacher/profile' },
        { icon: LogOut, label: 'navigation.logout', action: 'logout' },
      ],
    },
  ],
  STUDENT: [
    {
      title: 'navigation.sections.menu',
      items: [
        { icon: Home, label: 'navigation.home', href: '/student/dashboard' },
        { icon: Calendar, label: 'navigation.schedule', href: '/student/schedule' },
        { icon: FileText, label: 'navigation.exams', href: '/student/exams' },
        { icon: FileText, label: 'navigation.results', href: '/student/results' },
        { icon: Calendar, label: 'navigation.events', href: '/student/events' },
        { icon: Bell, label: 'navigation.announcements', href: '/student/announcements' },
      ],
    },
    {
      title: 'navigation.sections.other',
      items: [
        { icon: User, label: 'navigation.settings', href: '/student/profile' },
        { icon: LogOut, label: 'navigation.logout', action: 'logout' },
      ],
    },
  ],
  PARENT: [
    {
      title: 'navigation.sections.menu',
      items: [
        { icon: Home, label: 'navigation.home', href: '/parent/dashboard' },
        { icon: User2, label: 'navigation.children', href: '/parent/children' },
        { icon: GraduationCap, label: 'navigation.results', href: '/parent/grades' },
        { icon: Calendar, label: 'navigation.schedule', href: '/parent/schedule' },
        { icon: CheckSquare, label: 'navigation.attendance', href: '/parent/attendance' },
        { icon: Bell, label: 'navigation.announcements', href: '/parent/announcements' },
        { icon: CalendarCheck, label: 'navigation.meetings', href: '/parent/meetings' },
      ],
    },
    {
      title: 'navigation.sections.other',
      items: [
        { icon: User, label: 'navigation.settings', href: '/parent/profile' },
        { icon: LogOut, label: 'navigation.logout', action: 'logout' },
      ],
    },
  ],
  STAFF: [
    {
      title: 'navigation.sections.dashboard',
      items: [
        { icon: Home, label: 'navigation.overview', href: '/staff/dashboard' },

      ],
    },
    {
      title: 'navigation.sections.management',
      items: [
        { icon: Users, label: 'navigation.studentManagement', href: '/staff/students' },
        { icon: User, label: 'navigation.teacherManagement', href: '/staff/teachers' },
        { icon: User2, label: 'navigation.parentManagement', href: '/staff/parents' },
        { icon: ClipboardList, label: 'navigation.classManagement', href: '/staff/classes' },
        { icon: UserPlus, label: 'navigation.enrollments', href: '/staff/enrollments' },
        { icon: BarChart3, label: 'navigation.gradeManagement', href: '/staff/grades' },
        { icon: CheckSquare, label: 'navigation.attendanceManagement', href: '/staff/attendance' },
        { icon: Calendar, label: 'navigation.scheduleManagement', href: '/staff/schedule' },
        { icon: DoorOpen, label: 'navigation.roomManagement', href: '/staff/rooms' },

      ],
    },
    {
      title: 'navigation.sections.communication',
      items: [
        { icon: Bell, label: 'navigation.announcements', href: '/staff/announcements' },

      ],
    },
    {
      title: 'navigation.sections.account',
      items: [
        { icon: User, label: 'navigation.settings', href: '/staff/profile' },
      ],
    },
  ],
}; 