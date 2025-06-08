import {
  Home,
  School,
  User,
  Users,
  BookOpen,
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
  Mail,
  Layers,
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
        { icon: BookOpen, label: 'Subjects', href: '/admin/subjects' },
        { icon: BookMarked, label: 'Courses', href: '/admin/courses' },
        { icon: ClipboardList, label: 'Classes', href: '/admin/classes' },
        { icon: Layers, label: 'Levels', href: '/admin/levels' },
      ],
    },
    {
      title: 'OPERATIONS',
      items: [
        { icon: Calendar, label: 'Schedule', href: '/admin/schedule' },
        { icon: Megaphone, label: 'Announcements', href: '/admin/announcements' },
        { icon: Settings, label: 'Settings', href: '/admin/settings' },
      ],
    },
  ],
  TEACHER: [
    {
      title: 'DASHBOARD',
      items: [
        { icon: Home, label: 'Overview', href: '/teacher/dashboard' },
        { icon: BookMarked, label: 'My Classes', href: '/teacher/classes' },
      ],
    },
    {
      title: 'ACADEMIC',
      items: [
        { icon: FileText, label: 'Assignments', href: '/teacher/assignments' },
        { icon: ClipboardList, label: 'Grades', href: '/teacher/grades' },
        { icon: Calendar, label: 'Schedule', href: '/teacher/schedule' },
        { icon: Layers, label: 'Levels', href: '/teacher/levels' },
      ],
    },
    {
      title: 'COMMUNICATION',
      items: [
        { icon: MessageCircle, label: 'Messages', href: '/teacher/messages' },
        { icon: Bell, label: 'Announcements', href: '/teacher/announcements' },
      ],
    },
  ],
  STUDENT: [
    {
      title: 'DASHBOARD',
      items: [
        { icon: Home, label: 'Overview', href: '/student/dashboard' },
        { icon: BookMarked, label: 'My Courses', href: '/student/courses' },
      ],
    },
    {
      title: 'ACADEMIC',
      items: [
        { icon: FileText, label: 'Assignments', href: '/student/assignments' },
        { icon: GraduationCap, label: 'Grades', href: '/student/grades' },
        { icon: Calendar, label: 'Schedule', href: '/student/schedule' },
      ],
    },
    {
      title: 'COMMUNICATION',
      items: [
        { icon: MessageCircle, label: 'Messages', href: '/student/messages' },
        { icon: Bell, label: 'Announcements', href: '/student/announcements' },
      ],
    },
  ],
  PARENT: [
    {
      title: 'DASHBOARD',
      items: [
        { icon: Home, label: 'Overview', href: '/parent/dashboard' },
        { icon: User2, label: 'My Children', href: '/parent/children' },
      ],
    },
    {
      title: 'ACADEMIC',
      items: [
        { icon: GraduationCap, label: 'Grades', href: '/parent/grades' },
        { icon: Calendar, label: 'Schedule', href: '/parent/schedule' },
        { icon: FileText, label: 'Assignments', href: '/parent/assignments' },
      ],
    },
    {
      title: 'COMMUNICATION',
      items: [
        { icon: MessageCircle, label: 'Messages', href: '/parent/messages' },
        { icon: Bell, label: 'Announcements', href: '/parent/announcements' },
        { icon: Mail, label: 'Contact Teachers', href: '/parent/contact' },
      ],
    },
  ],
}; 