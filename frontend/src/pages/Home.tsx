import { BookOpen, Building2, GraduationCap, LogOut, UserRound, Users } from 'lucide-react'
import { Helmet } from 'react-helmet-async'
import { useTranslation } from 'react-i18next'
import { Link } from 'react-router-dom'
import { Button } from '@/components/ui/button'
import { dashboardPathFor } from '@/config/dashboardPaths'
import { useAuth } from '@/hooks/useAuth'
import { terminateSession } from '@/lib/session'

const audiences = [
  { key: 'student', icon: GraduationCap },
  { key: 'teacher', icon: BookOpen },
  { key: 'parent', icon: Users },
  { key: 'staff', icon: Building2 },
] as const

function PublicHome() {
  const { t } = useTranslation()

  return (
    <div className="flex flex-col gap-12">
      <section aria-labelledby="home-title" className="flex max-w-2xl flex-col items-start gap-4">
        <h1 id="home-title" className="text-3xl font-semibold tracking-tight sm:text-4xl">
          {t('home.public.title')}
        </h1>
        <p className="text-base text-muted-foreground sm:text-lg">{t('home.public.intro')}</p>
        <Button asChild size="lg" className="h-11 px-6">
          <Link to="/login">{t('home.logIn')}</Link>
        </Button>
        <p className="text-sm text-muted-foreground">{t('home.public.loginHint')}</p>
      </section>

      <section aria-labelledby="home-audiences" className="flex flex-col gap-4">
        <h2 id="home-audiences" className="text-xl font-semibold">
          {t('home.public.audiencesHeading')}
        </h2>
        <ul className="grid gap-4 sm:grid-cols-2">
          {audiences.map(({ key, icon: Icon }) => (
            <li key={key} className="flex gap-4 rounded-xl border border-border bg-card p-4 shadow-xs">
              <span className="flex size-10 shrink-0 items-center justify-center rounded-lg bg-primary-soft text-primary">
                <Icon className="size-5" aria-hidden="true" />
              </span>
              <div className="min-w-0">
                <h3 className="text-base font-semibold">{t(`home.public.audiences.${key}.title`)}</h3>
                <p className="mt-1 text-sm text-muted-foreground">{t(`home.public.audiences.${key}.description`)}</p>
              </div>
            </li>
          ))}
        </ul>
      </section>
    </div>
  )
}

function PersonalHome() {
  const { t } = useTranslation()
  const { user } = useAuth()

  const name = [user?.firstName, user?.lastName]
    .map((part) => part?.trim())
    .filter(Boolean)
    .join(' ')
  const dashboardPath = dashboardPathFor(user?.role)
  const roleLabel = dashboardPath && user ? t(`roles.${user.role.toLowerCase()}`) : null

  return (
    <section aria-labelledby="home-title" className="flex max-w-2xl flex-col items-start gap-4">
      {roleLabel && (
        <span className="inline-flex items-center gap-2 rounded-full bg-role-accent-soft px-3 py-1 text-sm font-medium">
          <span className="size-2 rounded-full bg-role-accent" aria-hidden="true" />
          {roleLabel}
        </span>
      )}
      <h1 id="home-title" className="break-words text-3xl font-semibold tracking-tight sm:text-4xl">
        {t('home.personal.welcome', { name: name || t('shell.account.fallbackName') })}
      </h1>
      {dashboardPath ? (
        <p className="text-base text-muted-foreground sm:text-lg">{t('home.personal.intro')}</p>
      ) : (
        <p role="status" className="text-base text-muted-foreground">
          {t('home.personal.unknownRole')}
        </p>
      )}
      <div role="group" aria-label={t('home.personal.shortcutsLabel')} className="flex flex-wrap items-center gap-3">
        {dashboardPath && (
          <Button asChild size="lg" className="h-11 px-6">
            <Link to={dashboardPath}>{t('home.personal.dashboard')}</Link>
          </Button>
        )}
        <Button asChild variant="outline" size="lg" className="h-11 px-6">
          <Link to="/profile">
            <UserRound aria-hidden="true" />
            {t('home.personal.profile')}
          </Link>
        </Button>
        <Button variant="ghost" size="lg" className="h-11 px-6" onClick={terminateSession}>
          <LogOut aria-hidden="true" />
          {t('home.personal.logout')}
        </Button>
      </div>
    </section>
  )
}

const Home = () => {
  const { t } = useTranslation()
  const { isAuthenticated } = useAuth()

  return (
    <>
      <Helmet>
        <title>{t('shell.appName')}</title>
      </Helmet>
      {isAuthenticated ? <PersonalHome /> : <PublicHome />}
    </>
  )
}

export default Home
