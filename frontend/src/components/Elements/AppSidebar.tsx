// src/components/AppSidebar.tsx
import {
  Sidebar,
  SidebarHeader,
  SidebarContent,
  SidebarSeparator,
  SidebarGroup,
  SidebarGroupLabel,
  SidebarGroupContent,
  SidebarMenu,
  SidebarMenuItem,
  SidebarMenuButton,
  SidebarTrigger,
  useSidebar,
} from '@/components/ui/sidebar'
import { ScrollArea } from '@/components/ui/scroll-area'
import { useTranslation } from 'react-i18next'
import { useNavigate, useLocation } from 'react-router-dom'
import { useUserRole } from '@/hooks/useUserRole'
import { menuConfig } from '@/config/menuConfig'

export const AppSidebar = () => {
  const { t } = useTranslation()
  const navigate = useNavigate()
  const location = useLocation()
  const userRole = useUserRole()
  const { state, isMobile } = useSidebar()

  const menuSections = menuConfig[userRole] || []

  return (
    <div className="flex">
      <Sidebar collapsible="icon" className="flex flex-col bg-gradient-to-b from-white to-gray-50 text-gray-800 border-r border-gray-200 shadow-lg">
        {/* Sidebar header */}
        <SidebarHeader className="flex items-center px-4 py-6 border-b border-gray-100">
          <div className="flex items-center gap-3">
            <div className="w-8 h-8 bg-gradient-to-br from-blue-500 to-purple-600 rounded-lg flex items-center justify-center">
              <span className="text-white font-bold text-sm">M</span>
            </div>
            {state === 'expanded' && !isMobile && (
              <span className="text-xl font-semibold bg-gradient-to-r from-gray-800 to-gray-600 bg-clip-text text-transparent">My App</span>
            )}
          </div>
        </SidebarHeader>

        <SidebarSeparator className="bg-gray-200" />

        <SidebarContent className="flex-1 p-0">
          <ScrollArea className="h-full">
            {menuSections.map((section, sectionIndex) => (
              <SidebarGroup key={sectionIndex} className="px-3 mb-6 last:mb-0">
                <SidebarGroupLabel className="text-xs font-semibold text-gray-500 uppercase tracking-wider mb-3">{t(section.title)}</SidebarGroupLabel>
                <SidebarGroupContent>
                  <SidebarMenu>
                    {section.items.map((item, itemIndex) => {
                      const isActive = location.pathname === item.href;
                      return (
                        <SidebarMenuItem key={itemIndex}>
                          <SidebarMenuButton 
                            asChild 
                            tooltip={t(item.label)}
                            isActive={isActive}
                          >
                            <button
                              onClick={() => navigate(item.href)}
                              className={`flex w-full items-center gap-3 px-3 py-3 rounded-xl transition-all duration-200 group ${
                                isActive 
                                  ? 'bg-gradient-to-r from-blue-100 to-blue-50 border-l-4 border-blue-600 shadow-md text-blue-900' 
                                  : 'hover:bg-gradient-to-r hover:from-blue-50 hover:to-purple-50 hover:border-l-4 hover:border-blue-500 hover:shadow-md'
                              }`}
                            >
                              <item.icon className={`h-5 w-5 flex-shrink-0 transition-colors duration-200 ${
                                isActive 
                                  ? 'text-blue-700' 
                                  : 'text-gray-600 group-hover:text-blue-600'
                              }`} />
                              <span className={`font-medium transition-colors duration-200 ${
                                isActive 
                                  ? 'text-blue-900 font-semibold' 
                                  : 'text-gray-700 group-hover:text-gray-900'
                              }`}>{t(item.label)}</span>
                            </button>
                          </SidebarMenuButton>
                        </SidebarMenuItem>
                      );
                    })}
                  </SidebarMenu>
                </SidebarGroupContent>
              </SidebarGroup>
            ))}
          </ScrollArea>
        </SidebarContent>
      </Sidebar>

      {/* Sidebar Trigger positioned outside the sidebar */}
      <div className="flex items-start pt-4">
        <SidebarTrigger className={`h-10 w-10 rounded-full bg-white border-2 border-gray-200 shadow-lg hover:shadow-xl transition-all duration-300 hover:bg-gradient-to-r hover:from-blue-50 hover:to-purple-50 hover:border-blue-300 ml-3 ${
          state === 'collapsed' ? '[&>svg]:rotate-180' : ''
        }`} />
      </div>
    </div>
  )
}
