/**
 * The dashboard each role lands on. Returns null for a role the application does not know,
 * so an unrecognised account is never sent to a privileged area.
 */
export function dashboardPathFor(role: string | null | undefined): string | null {
  switch (role) {
    case 'ADMIN':
      return '/admin/dashboard'
    case 'TEACHER':
      return '/teacher/dashboard'
    case 'STUDENT':
      return '/student/dashboard'
    case 'PARENT':
      return '/parent/dashboard'
    case 'STAFF':
      return '/staff/dashboard'
    default:
      return null
  }
}
