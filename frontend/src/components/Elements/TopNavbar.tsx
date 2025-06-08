import React, { useState, useEffect } from 'react'
import { Search, Settings, Bell, User, LogOut, ChevronDown } from 'lucide-react'
import { Button } from '@/components/ui/button'
import {
  DropdownMenu,
  DropdownMenuContent,
  DropdownMenuItem,
  DropdownMenuLabel,
  DropdownMenuSeparator,
  DropdownMenuTrigger,
} from '@/components/ui/dropdown-menu'
import {
  Dialog,
  DialogContent,
  DialogHeader,
  DialogTitle,
} from '@/components/ui/dialog'
import { Input } from '@/components/ui/input'
import { Badge } from '@/components/ui/badge'
import { ScrollArea } from '@/components/ui/scroll-area'
import { useTranslation } from 'react-i18next'
import { useNavigate } from 'react-router-dom'
import { useDispatch } from 'react-redux'
import { logout, resetAuth } from '@/stores/authSlice'
import { performLogout } from '@/lib/auth'
import { useUserRole } from '@/hooks/useUserRole'
import { menuConfig } from '@/config/menuConfig'

interface SearchItem {
  title: string
  description?: string
  href: string
  icon?: React.ComponentType<{ className?: string }>
}

export const TopNavbar = () => {
  const { t } = useTranslation()
  const navigate = useNavigate()
  const dispatch = useDispatch()
  const userRole = useUserRole()
  const [isSearchOpen, setIsSearchOpen] = useState(false)
  const [searchQuery, setSearchQuery] = useState('')
  const [searchResults, setSearchResults] = useState<SearchItem[]>([])

  // Mock notifications - replace with real data
  const notifications = [
    { id: 1, title: 'New message received', time: '2 min ago', unread: true },
    { id: 2, title: 'System update completed', time: '1 hour ago', unread: false },
    { id: 3, title: 'Backup completed successfully', time: '3 hours ago', unread: false },
  ]

  const unreadCount = notifications.filter(n => n.unread).length

  // Create searchable items from menu config
  const getAllSearchableItems = (): SearchItem[] => {
    const items: SearchItem[] = []
    const menuSections = menuConfig[userRole] || []
    
    menuSections.forEach(section => {
      section.items.forEach(item => {
        items.push({
          title: t(item.label),
          description: t(section.title),
          href: item.href,
          icon: item.icon
        })
      })
    })

    // Add common pages
    items.push(
      { title: t('Settings'), description: 'Application settings', href: '/settings' },
      { title: t('Profile'), description: 'User profile', href: '/profile' },
      { title: t('Help'), description: 'Help and support', href: '/help' }
    )

    return items
  }

  // Handle search
  useEffect(() => {
    if (searchQuery.trim()) {
      const allItems = getAllSearchableItems()
      const filtered = allItems.filter(item =>
        item.title.toLowerCase().includes(searchQuery.toLowerCase()) ||
        item.description?.toLowerCase().includes(searchQuery.toLowerCase())
      )
      setSearchResults(filtered.slice(0, 8)) // Limit to 8 results
    } else {
      setSearchResults([])
    }
  }, [searchQuery, userRole, t])

  // Handle Ctrl+K shortcut
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

  const handleLogout = () => {
    performLogout()
    dispatch(logout())
    dispatch(resetAuth())
    navigate('/login')
  }

  const handleSearchItemClick = (href: string) => {
    navigate(href)
    setIsSearchOpen(false)
    setSearchQuery('')
  }

    return (
    <>
      <header className="sticky top-0 z-50 w-full ml-6 rounded-bl-lg ">
        {/* Background with rounded bottom-right corner */}
        <div className="relative bg-gradient-to-r from-white via-blue-50/30 to-purple-50/30 border-b border-gray-200/60 shadow-lg backdrop-blur supports-[backdrop-filter]:bg-white/95 rounded-bl-lg">
          
          {/* Main navbar content */}
          <div className="relative z-10 flex h-16 items-center justify-between pl-6 pr-8 bg-gradient-to-r from-white/90 via-blue-50/40 to-purple-50/40 rounded-br-2xl shadow-md border-r border-gray-100 rounded-bl-lg" >
{/* Center - Search */}
          <div className="flex-1 max-w-md ml-0 mr-4">
            <Button
              variant="outline"
              className="relative w-full justify-start text-sm text-muted-foreground border-gray-200 bg-gray-50/50 hover:bg-white hover:border-blue-200 hover:shadow-md transition-all duration-200"
              onClick={() => setIsSearchOpen(true)}
            >
              <Search className="mr-2 h-4 w-4 text-gray-500" />
              <span className="inline-flex text-gray-600">Search...</span>
              <div className="ml-auto flex items-center space-x-1">
                <kbd className="pointer-events-none inline-flex h-5 select-none items-center gap-1 rounded border bg-gray-100 px-1.5 font-mono text-[10px] font-medium text-gray-500 opacity-100 shadow-sm">
                  <span className="text-xs">⌘</span>K
                </kbd>
              </div>
            </Button>
          </div>

          {/* Right side - Actions */}
          <div className="flex items-center space-x-3">
            {/* Notifications */}
            <DropdownMenu>
              <DropdownMenuTrigger asChild>
                <Button variant="ghost" size="icon" className="relative hover:bg-gray-100 hover:shadow-md transition-all duration-200 rounded-xl">
                  <Bell className="h-5 w-5 text-gray-600" />
                  {unreadCount > 0 && (
                    <Badge className="absolute -top-1 -right-1 h-5 w-5 p-0 text-xs flex items-center justify-center leading-none text-center text-white bg-gradient-to-r from-red-500 to-pink-500 shadow-md">
                        {unreadCount}
                    </Badge>
                  )}
                </Button>
              </DropdownMenuTrigger>
              <DropdownMenuContent align="end" className="w-80 bg-white border-gray-200 shadow-xl rounded-xl">
                <DropdownMenuLabel className="text-gray-800 font-semibold">Notifications</DropdownMenuLabel>
                <DropdownMenuSeparator className="bg-gray-200" />
                <ScrollArea className="h-64">
                  {notifications.map((notification) => (
                    <DropdownMenuItem key={notification.id} className="flex flex-col items-start p-3 hover:bg-gradient-to-r hover:from-gray-50 hover:to-blue-50 transition-all duration-200 rounded-lg mx-2 my-1">
                      <div className="flex w-full items-center justify-between">
                        <span className={`text-sm ${notification.unread ? 'font-medium text-gray-900' : 'text-gray-700'}`}>
                          {notification.title}
                        </span>
                        {notification.unread && (
                          <div className="h-2 w-2 rounded-full bg-gradient-to-r from-blue-500 to-purple-500 shadow-sm" />
                        )}
                      </div>
                      <span className="text-xs text-gray-500">{notification.time}</span>
                    </DropdownMenuItem>
                  ))}
                </ScrollArea>
              </DropdownMenuContent>
            </DropdownMenu>


            {/* User Profile */}
            <DropdownMenu>
              <DropdownMenuTrigger asChild>
                <Button variant="ghost" className="flex items-center space-x-2 px-3 hover:bg-gray-100 hover:shadow-md transition-all duration-200 rounded-xl">
                  <div className="w-8 h-8 bg-gradient-to-br from-blue-500 to-purple-600 rounded-full flex items-center justify-center">
                    <User className="h-4 w-4 text-white" />
                  </div>
                  <span className="hidden md:block font-medium text-gray-700">John Doe</span>
                  <ChevronDown className="h-4 w-4 text-gray-500" />
                </Button>
              </DropdownMenuTrigger>
              <DropdownMenuContent align="end" className="bg-white border-gray-200 shadow-xl rounded-xl">
                <DropdownMenuLabel className="text-gray-800 font-semibold">My Account</DropdownMenuLabel>
                <DropdownMenuSeparator className="bg-gray-200" />
                <DropdownMenuItem onClick={() => navigate('/profile')} className="hover:bg-gradient-to-r hover:from-gray-50 hover:to-blue-50 transition-all duration-200 rounded-lg mx-1">
                  <User className="mr-2 h-4 w-4 text-gray-600" />
                  <span className="text-gray-700">Profile</span>
                </DropdownMenuItem>
                <DropdownMenuItem onClick={() => navigate('/settings')} className="hover:bg-gradient-to-r hover:from-gray-50 hover:to-blue-50 transition-all duration-200 rounded-lg mx-1">
                  <Settings className="mr-2 h-4 w-4 text-gray-600" />
                  <span className="text-gray-700">Settings</span>
                </DropdownMenuItem>
                <DropdownMenuSeparator className="bg-gray-200" />
                <DropdownMenuItem onClick={handleLogout} className="text-red-600 hover:bg-gradient-to-r hover:from-red-50 hover:to-pink-50 transition-all duration-200 rounded-lg mx-1">
                  <LogOut className="mr-2 h-4 w-4" />
                  Sign out
                </DropdownMenuItem>
              </DropdownMenuContent>
            </DropdownMenu>
          </div>
          </div>
        </div>
      </header>

      {/* Search Command Palette */}
      <Dialog open={isSearchOpen} onOpenChange={setIsSearchOpen}>
        <DialogContent className="max-w-2xl p-0 border-gray-200 shadow-2xl rounded-2xl">
          <DialogHeader className="p-4 pb-2 border-b border-gray-100">
            <DialogTitle className="sr-only">Search</DialogTitle>
            <div className="relative">
              <Search className="absolute left-3 top-1/2 h-4 w-4 -translate-y-1/2 text-gray-400" />
              <Input
                placeholder="Search for pages, features..."
                value={searchQuery}
                onChange={(e) => setSearchQuery(e.target.value)}
                className="pl-10 border-gray-200 focus:border-blue-300 focus:ring-blue-200 rounded-xl"
                autoFocus
              />
            </div>
          </DialogHeader>
          <ScrollArea className="max-h-96">
            {searchResults.length > 0 ? (
              <div className="p-2">
                {searchResults.map((item, index) => (
                  <Button
                    key={index}
                    variant="ghost"
                    className="w-full justify-start p-3 h-auto hover:bg-gradient-to-r hover:from-gray-50 hover:to-blue-50 transition-all duration-200 rounded-xl mx-1 my-1"
                    onClick={() => handleSearchItemClick(item.href)}
                  >
                    <div className="flex items-center space-x-3">
                      {item.icon && <item.icon className="h-5 w-5 text-gray-600" />}
                      <div className="flex flex-col items-start">
                        <span className="font-medium text-gray-800">{item.title}</span>
                        {item.description && (
                          <span className="text-sm text-gray-500">{item.description}</span>
                        )}
                      </div>
                    </div>
                  </Button>
                ))}
              </div>
            ) : searchQuery.trim() ? (
              <div className="p-8 text-center text-gray-500">
                No results found for "{searchQuery}"
              </div>
            ) : (
              <div className="p-8 text-center text-gray-500">
                Type to search for pages and features...
              </div>
            )}
          </ScrollArea>
        </DialogContent>
      </Dialog>
    </>
  )
} 