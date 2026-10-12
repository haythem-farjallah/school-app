import { ChevronDown, GraduationCap, Menu, User } from 'lucide-react'
import type { ReactNode } from 'react'
import { useTranslation } from 'react-i18next'
import { Link, Outlet, useLocation } from 'react-router-dom'
import { Button } from '@/components/ui/button'
import {
  DropdownMenu,
  DropdownMenuContent,
  DropdownMenuItem,
  DropdownMenuLabel,
  DropdownMenuSeparator,
  DropdownMenuTrigger,
} from '@/components/ui/dropdown-menu'
import { menuConfig, type MenuItem } from '@/config/menuConfig'
import { useAuth } from '@/hooks/useAuth'
import { terminateSession } from '@/lib/session'
import { cn } from '@/lib/utils'

type LinkItem = Extract<MenuItem, { href: string }>

const isLink = (item: MenuItem): item is LinkItem => 'href' in item

// The student menu is defined once in menuConfig: its first section is the top navigation,
// its second the account menu.
const [navigationSection, accountSection] = menuConfig.STUDENT
const navigationLinks = navigationSection.items.filter(isLink)
const accountLinks = accountSection.items.filter(isLink)
const accountActions = accountSection.items.filter((item) => !isLink(item))

// "/" is the home page and would otherwise match every path.
const isActivePath = (pathname: string, href: string) =>
  href === '/' ? pathname === '/' : pathname === href || pathname.startsWith(`${href}/`)

const focusRing =
  'focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-ring focus-visible:ring-offset-2 focus-visible:ring-offset-card'

function initialsOf(firstName?: string, lastName?: string) {
  return `${firstName?.charAt(0) ?? ''}${lastName?.charAt(0) ?? ''}`.toUpperCase()
}

/**
 * Shell of the student area: a top navigation instead of the staff sidebar.
 * Renders its children inside its single main landmark, or the matched child route when
 * it is used as a layout route.
 */
export const StudentShell = ({ children }: { children?: ReactNode }) => {
  const { t, i18n } = useTranslation()
  const { pathname } = useLocation()
  const { user } = useAuth()

  const fullName = user ? `${user.firstName} ${user.lastName}`.trim() : t('shell.account.fallbackName')
  const initials = initialsOf(user?.firstName, user?.lastName)
  // Radix alignment is physical; keep menus under their trigger at the inline end in RTL too.
  const menuAlign = i18n.dir() === 'rtl' ? 'start' : 'end'

  return (
    <div className="flex min-h-screen flex-col bg-background text-foreground">
      <a
        href="#main-content"
        className="sr-only focus:not-sr-only focus:fixed focus:start-4 focus:top-4 focus:z-50 focus:rounded-lg focus:bg-primary focus:px-4 focus:py-2 focus:text-sm focus:font-medium focus:text-primary-foreground focus:outline-none focus:ring-2 focus:ring-ring focus:ring-offset-2"
      >
        {t('home.skipToContent')}
      </a>

      <header className="sticky top-0 z-30 border-b border-border bg-card">
        <div className="mx-auto flex h-16 w-full max-w-6xl items-center gap-2 px-4 sm:gap-4 sm:px-6">
          <Link to="/" className={cn('flex min-w-0 items-center gap-3 rounded-lg', focusRing)}>
            <span className="flex size-9 shrink-0 items-center justify-center rounded-lg bg-primary text-primary-foreground">
              <GraduationCap className="size-5" aria-hidden="true" />
            </span>
            <span className="truncate text-base font-semibold">{t('shell.appName')}</span>
          </Link>

          <nav aria-label={t('shell.navigationLabel')} className="ms-2 hidden md:block">
            <ul className="flex items-center gap-1">
              {navigationLinks.map((item) => {
                const active = isActivePath(pathname, item.href)
                return (
                  <li key={item.href}>
                    <Link
                      to={item.href}
                      aria-current={active ? 'page' : undefined}
                      className={cn(
                        'flex h-10 items-center gap-2 rounded-lg px-3 text-sm font-medium transition-colors',
                        focusRing,
                        active
                          ? 'bg-primary-soft font-semibold text-primary'
                          : 'text-muted-foreground hover:bg-muted hover:text-foreground',
                      )}
                    >
                      <item.icon className="size-4 shrink-0" aria-hidden="true" />
                      <span>{t(item.label)}</span>
                    </Link>
                  </li>
                )
              })}
            </ul>
          </nav>

          <div className="ms-auto flex shrink-0 items-center gap-1 sm:gap-2">
            <DropdownMenu>
              <DropdownMenuTrigger asChild>
                <Button
                  variant="ghost"
                  size="icon"
                  className={cn('size-11 md:hidden [&_svg]:size-5', focusRing)}
                  aria-label={t('shell.sidebar.open')}
                >
                  <Menu aria-hidden="true" />
                </Button>
              </DropdownMenuTrigger>
              <DropdownMenuContent align={menuAlign} className="w-60 max-w-[calc(100vw-2rem)] rounded-xl p-1.5 shadow-overlay">
                {navigationLinks.map((item) => {
                  const active = isActivePath(pathname, item.href)
                  return (
                    <DropdownMenuItem
                      key={item.href}
                      asChild
                      className={cn(
                        'min-h-11 cursor-pointer gap-3 rounded-lg px-3',
                        active && 'bg-primary-soft font-semibold text-primary',
                      )}
                    >
                      <Link to={item.href} aria-current={active ? 'page' : undefined}>
                        <item.icon className="size-4 shrink-0" aria-hidden="true" />
                        <span>{t(item.label)}</span>
                      </Link>
                    </DropdownMenuItem>
                  )
                })}
              </DropdownMenuContent>
            </DropdownMenu>

            <DropdownMenu>
              <DropdownMenuTrigger asChild>
                <Button
                  variant="ghost"
                  className={cn('h-11 w-auto gap-2 px-1.5 md:h-10 lg:pe-2.5', focusRing)}
                  aria-label={t('shell.account.menuLabel', { name: fullName })}
                >
                  <span className="flex size-8 shrink-0 items-center justify-center rounded-full bg-role-accent-soft text-xs font-semibold text-foreground ring-2 ring-role-accent">
                    {initials || <User className="!size-4" aria-hidden="true" />}
                  </span>
                  <span className="hidden max-w-40 truncate font-medium lg:inline">{fullName}</span>
                  <ChevronDown className="hidden !size-4 text-muted-foreground sm:block" aria-hidden="true" />
                </Button>
              </DropdownMenuTrigger>
              <DropdownMenuContent align={menuAlign} className="w-64 max-w-[calc(100vw-2rem)] rounded-xl p-1.5 shadow-overlay">
                <DropdownMenuLabel className="flex flex-col gap-0.5 px-2 py-2 font-normal">
                  <span className="truncate text-sm font-semibold text-foreground">{fullName}</span>
                  {user?.email && <span className="truncate text-xs text-muted-foreground">{user.email}</span>}
                </DropdownMenuLabel>
                <DropdownMenuSeparator />
                {accountLinks.map((item) => (
                  <DropdownMenuItem key={item.href} asChild className="min-h-11 cursor-pointer gap-2 rounded-lg px-2">
                    <Link to={item.href}>
                      <item.icon className="size-4 shrink-0 text-muted-foreground" aria-hidden="true" />
                      <span>{t(item.label)}</span>
                    </Link>
                  </DropdownMenuItem>
                ))}
                <DropdownMenuSeparator />
                {accountActions.map((item) => (
                  <DropdownMenuItem
                    key={item.label}
                    onSelect={terminateSession}
                    className="min-h-11 cursor-pointer gap-2 rounded-lg px-2 text-destructive focus:bg-destructive/10 focus:text-destructive"
                  >
                    <item.icon className="size-4 shrink-0" aria-hidden="true" />
                    <span>{t(item.label)}</span>
                  </DropdownMenuItem>
                ))}
              </DropdownMenuContent>
            </DropdownMenu>
          </div>
        </div>
      </header>

      <main id="main-content" tabIndex={-1} className="mx-auto w-full max-w-6xl min-w-0 flex-1 px-4 py-6 focus:outline-none sm:px-6 sm:py-8">
        {children ?? <Outlet />}
      </main>
    </div>
  )
}
