import { GraduationCap } from 'lucide-react'
import { useTranslation } from 'react-i18next'
import { Link, Outlet } from 'react-router-dom'
import { Button } from '@/components/ui/button'
import { StudentShell } from './StudentShell'
import { useAuth } from '@/hooks/useAuth'

/**
 * Shell of the home page, shared by visitors and signed-in users. It has no application sidebar.
 * A signed-in student gets the same top navigation as in the student area.
 */
export const HomeLayout = () => {
  const { t } = useTranslation()
  const { isAuthenticated, user } = useAuth()

  if (isAuthenticated && user?.role === 'STUDENT') {
    return (
      <StudentShell>
        <Outlet />
      </StudentShell>
    )
  }

  return (
    <div className="flex min-h-screen flex-col bg-background text-foreground">
      <a
        href="#main-content"
        className="sr-only focus:not-sr-only focus:fixed focus:start-4 focus:top-4 focus:z-50 focus:rounded-lg focus:bg-primary focus:px-4 focus:py-2 focus:text-sm focus:font-medium focus:text-primary-foreground focus:outline-none focus:ring-2 focus:ring-ring focus:ring-offset-2"
      >
        {t('home.skipToContent')}
      </a>
      <header className="border-b border-border bg-card">
        <div className="mx-auto flex h-16 w-full max-w-5xl items-center justify-between gap-4 px-4 sm:px-6">
          <Link
            to="/"
            className="flex min-w-0 items-center gap-3 rounded-lg focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-ring focus-visible:ring-offset-2"
          >
            <span className="flex size-9 shrink-0 items-center justify-center rounded-lg bg-primary text-primary-foreground">
              <GraduationCap className="size-5" aria-hidden="true" />
            </span>
            <span className="truncate text-base font-semibold">{t('shell.appName')}</span>
          </Link>
          {!isAuthenticated && (
            <Button asChild className="h-11 md:h-10">
              <Link to="/login">{t('home.logIn')}</Link>
            </Button>
          )}
        </div>
      </header>
      <main id="main-content" tabIndex={-1} className="mx-auto w-full max-w-5xl flex-1 px-4 py-8 focus:outline-none sm:px-6 sm:py-12">
        <Outlet />
      </main>
    </div>
  )
}
