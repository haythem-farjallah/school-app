import React, { useEffect, useMemo, useState } from 'react'
import {
  BookOpen,
  ChevronDown,
  CornerDownLeft,
  LogOut,
  Menu,
  PanelLeftClose,
  PanelLeftOpen,
  Search,
  Settings,
  User,
} from 'lucide-react'
import { NotificationBell } from '@/features/notifications/components/NotificationBell'
import { Button } from '@/components/ui/button'
import {
  DropdownMenu,
  DropdownMenuContent,
  DropdownMenuItem,
  DropdownMenuLabel,
  DropdownMenuSeparator,
  DropdownMenuTrigger,
} from '@/components/ui/dropdown-menu'
import { Dialog, DialogContent, DialogDescription, DialogTitle } from '@/components/ui/dialog'
import { useSidebar } from '@/components/ui/sidebar'
import { useTranslation } from 'react-i18next'
import { Link, useNavigate } from 'react-router-dom'
import { terminateSession } from '@/lib/session'
import { usePermissions, useUserRole } from '@/hooks/useUserRole'
import { useAuth } from '@/hooks/useAuth'
import { getMenuSections } from '@/config/menuConfig'
import { cn } from '@/lib/utils'

interface SearchItem {
  title: string
  section: string
  href: string
  icon: React.ComponentType<{ className?: string }>
}

// Icon-led controls on the brand bar: light foreground, focus ring visible against teal.
const barButton =
  'h-11 w-11 shrink-0 rounded-lg text-primary-foreground hover:bg-primary-foreground/15 hover:text-primary-foreground ' +
  'focus-visible:ring-primary-foreground focus-visible:ring-offset-primary md:h-10 md:w-10 [&_svg]:size-5'

const isMac = typeof navigator !== 'undefined' && /Mac|iPhone|iPad/.test(navigator.userAgent)

// Menu section titles are upper-case labels; search shows them as quieter secondary text.
function sentenceCase(text: string) {
  return text === text.toUpperCase() ? text.charAt(0) + text.slice(1).toLowerCase() : text
}

function initialsOf(firstName?: string, lastName?: string) {
  return `${firstName?.charAt(0) ?? ''}${lastName?.charAt(0) ?? ''}`.toUpperCase()
}

export const TopNavbar = () => {
  const { t } = useTranslation()
  const navigate = useNavigate()
  const userRole = useUserRole()
  const perms = usePermissions()
  const { user } = useAuth()
  const { isMobile, open, openMobile, toggleSidebar } = useSidebar()
  const [isSearchOpen, setIsSearchOpen] = useState(false)
  const [searchQuery, setSearchQuery] = useState('')
  const [selectedIndex, setSelectedIndex] = useState(0)

  const fullName = user ? `${user.firstName} ${user.lastName}` : 'User'
  const initials = initialsOf(user?.firstName, user?.lastName)
  const roleLabel = user?.role ? user.role.charAt(0) + user.role.slice(1).toLowerCase() : null

  // Search offers exactly the pages this user can navigate to; actions such as logout are not listed.
  const searchItems = useMemo((): SearchItem[] => {
    const items: SearchItem[] = []
    for (const section of getMenuSections(userRole, perms)) {
      for (const item of section.items) {
        if ('href' in item) {
          items.push({ title: t(item.label), section: sentenceCase(t(section.title)), href: item.href, icon: item.icon })
        }
      }
    }
    items.push(
      { title: t('Learning Space'), section: t('General'), href: '/learning-space', icon: BookOpen },
      { title: t('Profile settings'), section: t('Account'), href: '/profile', icon: Settings },
    )
    return items.filter((item, index) => items.findIndex((other) => other.href === item.href) === index)
  }, [userRole, perms, t])

  const searchResults = useMemo(() => {
    const query = searchQuery.trim().toLowerCase()
    if (!query) return searchItems
    return searchItems.filter(
      (item) => item.title.toLowerCase().includes(query) || item.section.toLowerCase().includes(query),
    )
  }, [searchItems, searchQuery])

  const setSearchOpen = (nextOpen: boolean) => {
    setIsSearchOpen(nextOpen)
    if (!nextOpen) {
      setSearchQuery('')
      setSelectedIndex(0)
    }
  }

  // Ctrl/Cmd + K opens search from anywhere in the shell.
  useEffect(() => {
    const handleKeyDown = (e: KeyboardEvent) => {
      if ((e.ctrlKey || e.metaKey) && e.key === 'k') {
        e.preventDefault()
        setIsSearchOpen(true)
      }
    }

    document.addEventListener('keydown', handleKeyDown)
    return () => document.removeEventListener('keydown', handleKeyDown)
  }, [])

  // Keep the keyboard selection visible while moving through a long list.
  useEffect(() => {
    if (isSearchOpen) document.getElementById(`shell-search-option-${selectedIndex}`)?.scrollIntoView?.({ block: 'nearest' })
  }, [isSearchOpen, selectedIndex])

  const openSearchResult = (href: string) => {
    navigate(href)
    setSearchOpen(false)
  }

  const handleSearchKeyDown = (e: React.KeyboardEvent) => {
    switch (e.key) {
      case 'ArrowDown':
        e.preventDefault()
        setSelectedIndex((prev) => Math.min(prev + 1, searchResults.length - 1))
        break
      case 'ArrowUp':
        e.preventDefault()
        setSelectedIndex((prev) => Math.max(prev - 1, 0))
        break
      case 'Enter':
        e.preventDefault()
        if (searchResults[selectedIndex]) openSearchResult(searchResults[selectedIndex].href)
        break
    }
  }

  const sidebarToggleLabel = isMobile ? 'Open navigation menu' : open ? 'Collapse sidebar' : 'Expand sidebar'

  return (
    <>
      <div className="sticky top-0 z-30 bg-background px-4 pt-3 sm:px-6 lg:px-8 lg:pt-4">
        <header className="flex h-16 items-center gap-1 rounded-xl bg-primary px-2 text-primary-foreground shadow-sm sm:gap-2 sm:px-3">
          <Button
            variant="ghost"
            size="icon"
            className={barButton}
            onClick={toggleSidebar}
            aria-label={sidebarToggleLabel}
            aria-expanded={isMobile ? openMobile : open}
          >
            {isMobile ? <Menu /> : open ? <PanelLeftClose /> : <PanelLeftOpen />}
          </Button>

          <button
            type="button"
            onClick={() => setSearchOpen(true)}
            className="ml-1 hidden h-10 w-full max-w-md items-center gap-2 rounded-lg bg-card px-3 text-sm text-muted-foreground shadow-xs transition-colors hover:bg-input-background focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-primary-foreground focus-visible:ring-offset-2 focus-visible:ring-offset-primary md:flex"
          >
            <Search className="size-4 shrink-0" aria-hidden="true" />
            <span className="truncate">Search pages…</span>
            <kbd className="pointer-events-none ml-auto hidden select-none rounded border border-border bg-muted px-1.5 font-sans text-xs font-medium text-muted-foreground lg:inline-block">
              {isMac ? '⌘K' : 'Ctrl K'}
            </kbd>
          </button>
          <Button
            variant="ghost"
            size="icon"
            className={cn(barButton, 'md:hidden')}
            onClick={() => setSearchOpen(true)}
            aria-label="Search pages"
          >
            <Search />
          </Button>

          <div className="ml-auto flex items-center gap-1 sm:gap-2">
            {/* The bell keeps its own neutral styling, so it sits on a light surface. */}
            <div className="rounded-lg bg-card text-foreground">
              <NotificationBell />
            </div>

            <Button asChild variant="ghost" className={cn(barButton, 'w-auto px-3 md:w-auto')}>
              <Link to="/learning-space" aria-label="Learning Space">
                <BookOpen aria-hidden="true" />
                <span className="hidden lg:inline">Learning Space</span>
              </Link>
            </Button>

            <DropdownMenu>
              <DropdownMenuTrigger asChild>
                <Button
                  variant="ghost"
                  className={cn(barButton, 'w-auto gap-2 px-1.5 md:w-auto lg:pr-2.5')}
                  aria-label={`Account menu, ${fullName}`}
                >
                  <span className="flex size-8 shrink-0 items-center justify-center rounded-full bg-primary-foreground text-xs font-semibold text-primary">
                    {initials || <User className="!size-4" aria-hidden="true" />}
                  </span>
                  <span className="hidden max-w-40 truncate font-medium lg:inline">{fullName}</span>
                  <ChevronDown className="hidden !size-4 opacity-80 sm:block" aria-hidden="true" />
                </Button>
              </DropdownMenuTrigger>
              <DropdownMenuContent align="end" className="w-64 rounded-xl p-1.5 shadow-overlay">
                <DropdownMenuLabel className="flex items-start gap-3 px-2 py-2 font-normal">
                  <span className="flex size-10 shrink-0 items-center justify-center rounded-full bg-primary-soft text-sm font-semibold text-primary">
                    {initials || <User className="size-5" aria-hidden="true" />}
                  </span>
                  <span className="flex min-w-0 flex-col gap-0.5">
                    <span className="truncate text-sm font-semibold text-foreground">{fullName}</span>
                    {user?.email && <span className="truncate text-xs text-muted-foreground">{user.email}</span>}
                    {roleLabel && (
                      <span className="mt-1 inline-flex w-fit items-center gap-1.5 rounded-full bg-role-accent-soft px-2 py-0.5 text-xs font-medium text-foreground">
                        <span className="size-2 rounded-full bg-role-accent" aria-hidden="true" />
                        {roleLabel}
                      </span>
                    )}
                  </span>
                </DropdownMenuLabel>
                <DropdownMenuSeparator />
                <DropdownMenuItem onClick={() => navigate('/profile')} className="cursor-pointer gap-2 rounded-lg px-2 py-2">
                  <Settings className="size-4 text-muted-foreground" aria-hidden="true" />
                  Profile settings
                </DropdownMenuItem>
                <DropdownMenuSeparator />
                <DropdownMenuItem
                  onClick={terminateSession}
                  className="cursor-pointer gap-2 rounded-lg px-2 py-2 text-destructive focus:bg-destructive/10 focus:text-destructive"
                >
                  <LogOut className="size-4" aria-hidden="true" />
                  Sign out
                </DropdownMenuItem>
              </DropdownMenuContent>
            </DropdownMenu>
          </div>
        </header>
      </div>

      <Dialog open={isSearchOpen} onOpenChange={setSearchOpen}>
        <DialogContent className="w-[calc(100%-2rem)] max-w-xl gap-0 overflow-hidden rounded-xl border-border bg-card p-0 shadow-overlay [&>button]:top-5">
          <DialogTitle className="sr-only">Search pages</DialogTitle>
          <DialogDescription className="sr-only">Type to filter, use the arrow keys to choose a page and press Enter to open it.</DialogDescription>
          <div className="flex items-center gap-3 border-b border-border pl-4 pr-12">
            <Search className="size-5 shrink-0 text-muted-foreground" aria-hidden="true" />
            <input
              role="combobox"
              aria-label="Search pages"
              aria-expanded="true"
              aria-controls="shell-search-results"
              aria-activedescendant={searchResults.length > 0 ? `shell-search-option-${selectedIndex}` : undefined}
              aria-autocomplete="list"
              placeholder="Search pages…"
              value={searchQuery}
              onChange={(e) => {
                setSearchQuery(e.target.value)
                setSelectedIndex(0)
              }}
              onKeyDown={handleSearchKeyDown}
              className="h-14 w-full min-w-0 bg-transparent text-base text-foreground outline-none placeholder:text-muted-foreground"
            />
          </div>

          <div className="max-h-[min(24rem,60vh)] overflow-y-auto p-2">
            {searchResults.length > 0 ? (
              <ul id="shell-search-results" role="listbox" aria-label="Pages" className="flex flex-col gap-0.5">
                {searchResults.map((item, index) => {
                  const selected = index === selectedIndex
                  return (
                    <li
                      key={item.href}
                      id={`shell-search-option-${index}`}
                      role="option"
                      aria-selected={selected}
                      onMouseMove={() => setSelectedIndex(index)}
                      onClick={() => openSearchResult(item.href)}
                      className={cn(
                        'flex cursor-pointer items-center gap-3 rounded-lg px-3 py-2',
                        selected ? 'bg-primary-soft' : 'hover:bg-muted',
                      )}
                    >
                      <span
                        className={cn(
                          'flex size-9 shrink-0 items-center justify-center rounded-lg',
                          selected ? 'bg-card text-primary' : 'bg-muted text-muted-foreground',
                        )}
                      >
                        <item.icon className="size-[1.125rem]" />
                      </span>
                      <span className="flex min-w-0 flex-col">
                        <span className={cn('truncate text-sm font-medium', selected ? 'text-primary' : 'text-foreground')}>
                          {item.title}
                        </span>
                        <span className="truncate text-xs text-muted-foreground">{item.section}</span>
                      </span>
                      {selected && <CornerDownLeft className="ml-auto size-4 shrink-0 text-primary" aria-hidden="true" />}
                    </li>
                  )
                })}
              </ul>
            ) : (
              <p id="shell-search-results" className="px-3 py-10 text-center text-sm text-muted-foreground">
                No pages match “<span className="font-medium text-foreground">{searchQuery.trim()}</span>”
              </p>
            )}
          </div>

          <div className="hidden items-center gap-4 border-t border-border px-4 py-2.5 text-xs text-muted-foreground sm:flex">
            <span className="flex items-center gap-1.5">
              <kbd className="rounded border border-border bg-muted px-1.5 font-sans">↑</kbd>
              <kbd className="rounded border border-border bg-muted px-1.5 font-sans">↓</kbd>
              to navigate
            </span>
            <span className="flex items-center gap-1.5">
              <kbd className="rounded border border-border bg-muted px-1.5 font-sans">Enter</kbd>
              to open
            </span>
            <span className="flex items-center gap-1.5">
              <kbd className="rounded border border-border bg-muted px-1.5 font-sans">Esc</kbd>
              to close
            </span>
          </div>
        </DialogContent>
      </Dialog>
    </>
  )
}
