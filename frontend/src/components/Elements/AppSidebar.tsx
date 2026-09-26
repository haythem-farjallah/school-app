import {
  Sidebar,
  SidebarHeader,
  SidebarContent,
  SidebarGroup,
  SidebarGroupLabel,
  SidebarGroupContent,
  SidebarMenu,
  SidebarMenuItem,
  SidebarMenuButton,
  useSidebar,
} from '@/components/ui/sidebar'
import { ScrollArea } from '@/components/ui/scroll-area'
import { GraduationCap } from 'lucide-react'
import { useTranslation } from 'react-i18next'
import { Link, useLocation } from 'react-router-dom'
import { usePermissions, useUserRole } from '@/hooks/useUserRole'
import { getMenuSections } from '@/config/menuConfig'
import { terminateSession } from '@/lib/session'
import { cn } from '@/lib/utils'

/** Temporary product name; the application has no final brand yet. */
const APP_NAME = 'School App'

const itemClassName = cn(
  'relative h-10 gap-3 rounded-lg px-3 font-medium text-sidebar-foreground',
  '[&>svg]:size-5 [&>svg]:text-muted-foreground',
  'hover:bg-muted hover:text-foreground',
  'data-[active=true]:bg-primary-soft data-[active=true]:font-semibold data-[active=true]:text-primary',
  'data-[active=true]:hover:bg-primary-soft data-[active=true]:hover:text-primary data-[active=true]:[&>svg]:text-primary',
  // A small brand bar marks the active destination in the expanded sidebar.
  'data-[active=true]:before:absolute data-[active=true]:before:inset-y-2 data-[active=true]:before:left-0',
  'data-[active=true]:before:w-1 data-[active=true]:before:rounded-full data-[active=true]:before:bg-primary',
  'group-data-[collapsible=icon]:!size-10 group-data-[collapsible=icon]:justify-center group-data-[collapsible=icon]:before:hidden',
)

function isActivePath(pathname: string, href: string) {
  return pathname === href || pathname.startsWith(`${href}/`)
}

export const AppSidebar = () => {
  const { t } = useTranslation()
  const location = useLocation()
  const userRole = useUserRole()
  const perms = usePermissions()
  const { isMobile, setOpenMobile } = useSidebar()

  const menuSections = getMenuSections(userRole, perms)
  const closeMobileMenu = () => {
    if (isMobile) setOpenMobile(false)
  }

  return (
    <Sidebar collapsible="icon" className="border-sidebar-border">
      <SidebarHeader className="mt-3 h-16 flex-row items-center gap-3 px-4 lg:mt-4 group-data-[collapsible=icon]:justify-center group-data-[collapsible=icon]:px-2">
        <div className="flex size-9 shrink-0 items-center justify-center rounded-lg bg-primary text-primary-foreground">
          <GraduationCap className="size-5" aria-hidden="true" />
        </div>
        <div className="flex min-w-0 flex-col group-data-[collapsible=icon]:hidden">
          <span className="truncate text-base font-semibold leading-tight text-foreground">{APP_NAME}</span>
          {userRole && (
            <span className="flex items-center gap-1.5 text-xs text-muted-foreground">
              <span className="size-2 rounded-full bg-role-accent" aria-hidden="true" />
              <span className="capitalize">{userRole.toLowerCase()}</span>
            </span>
          )}
        </div>
      </SidebarHeader>

      <SidebarContent className="p-0">
        <ScrollArea className="h-full">
          <nav aria-label="Main navigation" className="pb-4">
            {menuSections.map((section) => (
              <SidebarGroup key={section.title} className="px-2 py-2">
                <SidebarGroupLabel className="px-3 text-[0.6875rem] font-semibold uppercase tracking-wider text-muted-foreground">
                  {t(section.title)}
                </SidebarGroupLabel>
                <SidebarGroupContent>
                  <SidebarMenu className="gap-0.5">
                    {section.items.map((item) => {
                      const label = t(item.label)
                      if (!('href' in item)) {
                        return (
                          <SidebarMenuItem key={label}>
                            <SidebarMenuButton tooltip={label} className={itemClassName} onClick={terminateSession}>
                              <item.icon aria-hidden="true" />
                              <span>{label}</span>
                            </SidebarMenuButton>
                          </SidebarMenuItem>
                        )
                      }
                      const isActive = isActivePath(location.pathname, item.href)
                      return (
                        <SidebarMenuItem key={item.href}>
                          <SidebarMenuButton asChild tooltip={label} isActive={isActive} className={itemClassName}>
                            <Link
                              to={item.href}
                              aria-current={isActive ? 'page' : undefined}
                              onClick={closeMobileMenu}
                            >
                              <item.icon aria-hidden="true" />
                              <span>{label}</span>
                            </Link>
                          </SidebarMenuButton>
                        </SidebarMenuItem>
                      )
                    })}
                  </SidebarMenu>
                </SidebarGroupContent>
              </SidebarGroup>
            ))}
          </nav>
        </ScrollArea>
      </SidebarContent>
    </Sidebar>
  )
}
