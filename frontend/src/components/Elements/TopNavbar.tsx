import React, { useState, useEffect } from 'react'
import { Search, Settings, Bell, User, LogOut, ChevronDown, X } from 'lucide-react'
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
  const [selectedIndex, setSelectedIndex] = useState(0)

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
      setSelectedIndex(0) // Reset selection
    } else {
      setSearchResults([])
      setSelectedIndex(0)
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
    setSelectedIndex(0)
  }

  // Handle keyboard navigation
  const handleKeyDown = (e: React.KeyboardEvent) => {
    if (!isSearchOpen) return

    switch (e.key) {
      case 'ArrowDown':
        e.preventDefault()
        setSelectedIndex(prev => Math.min(prev + 1, searchResults.length - 1))
        break
      case 'ArrowUp':
        e.preventDefault()
        setSelectedIndex(prev => Math.max(prev - 1, 0))
        break
      case 'Enter':
        e.preventDefault()
        if (searchResults[selectedIndex]) {
          handleSearchItemClick(searchResults[selectedIndex].href)
        }
        break
      case 'Escape':
        e.preventDefault()
        setIsSearchOpen(false)
        break
    }
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
              className="relative w-full justify-start text-sm text-muted-foreground border-gray-200/80 bg-white/80 backdrop-blur-sm hover:bg-white hover:border-blue-300 hover:shadow-lg hover:scale-[1.02] transition-all duration-300 rounded-2xl group"
              onClick={() => setIsSearchOpen(true)}
            >
              <Search className="mr-3 h-4 w-4 text-blue-500 group-hover:text-blue-600 transition-colors duration-200" />
              <span className="inline-flex text-gray-600 group-hover:text-gray-700 transition-colors duration-200">Search...</span>
              <div className="ml-auto flex items-center space-x-1">
                <kbd className="pointer-events-none inline-flex h-6 select-none items-center gap-1 rounded-lg border border-gray-200 bg-gradient-to-b from-gray-50 to-gray-100 px-2 font-mono text-[10px] font-medium text-gray-600 shadow-sm group-hover:border-blue-200 group-hover:from-blue-50 group-hover:to-blue-100 transition-all duration-200">
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
        <DialogContent className="max-w-3xl p-0 border-0 shadow-2xl rounded-3xl bg-white/95 backdrop-blur-xl overflow-hidden">
          <div className="bg-gradient-to-br from-blue-50/80 via-white to-purple-50/80 border border-white/20 shadow-xl rounded-3xl">
            <DialogHeader className="p-6 pb-4 border-b border-gray-100/50">
              <DialogTitle className="sr-only">Search</DialogTitle>
              <div className="relative">
                <div className="absolute left-4 top-1/2 -translate-y-1/2 text-blue-500">
                  <Search className="h-5 w-5" />
                </div>
                <Input
                  placeholder="Search for pages, features..."
                  value={searchQuery}
                  onChange={(e) => setSearchQuery(e.target.value)}
                  onKeyDown={handleKeyDown}
                  className="pl-12 pr-4 py-4 text-lg border-0 bg-white/80 backdrop-blur-sm focus:bg-white focus:ring-2 focus:ring-blue-400/50 rounded-2xl shadow-sm transition-all duration-300 placeholder:text-gray-400"
                  autoFocus
                />
                {searchQuery && (
                  <Button
                    variant="ghost"
                    size="sm"
                    className="absolute right-2 top-1/2 -translate-y-1/2 h-8 w-8 p-0 hover:bg-gray-100 rounded-full"
                    onClick={() => setSearchQuery('')}
                  >
                    <X className="h-4 w-4" />
                  </Button>
                )}
              </div>
              
              {/* Quick hints */}
              <div className="flex items-center justify-between mt-4 text-xs text-gray-500">
                <div className="flex items-center space-x-4">
                  <span className="flex items-center space-x-1">
                    <kbd className="px-2 py-1 bg-gray-100 rounded text-gray-600">↑↓</kbd>
                    <span>Navigate</span>
                  </span>
                  <span className="flex items-center space-x-1">
                    <kbd className="px-2 py-1 bg-gray-100 rounded text-gray-600">⏎</kbd>
                    <span>Select</span>
                  </span>
                  <span className="flex items-center space-x-1">
                    <kbd className="px-2 py-1 bg-gray-100 rounded text-gray-600">Esc</kbd>
                    <span>Close</span>
                  </span>
                </div>
                <span className="text-gray-400">
                  {searchResults.length > 0 && `${searchResults.length} result${searchResults.length > 1 ? 's' : ''}`}
                </span>
              </div>
            </DialogHeader>
            
            <ScrollArea className="max-h-96">
              {searchResults.length > 0 ? (
                <div className="p-2">
                  <div className="text-xs font-medium text-gray-500 px-4 py-2 mb-2">
                    Search Results
                  </div>
                  {searchResults.map((item, index) => (
                    <Button
                      key={index}
                      variant="ghost"
                      className={`w-full justify-start p-4 h-auto transition-all duration-300 rounded-2xl mx-2 my-1 group border ${
                        index === selectedIndex
                          ? 'bg-gradient-to-r from-blue-100/80 to-purple-100/80 border-blue-300/50 shadow-md'
                          : 'border-transparent hover:bg-gradient-to-r hover:from-blue-50/80 hover:to-purple-50/80 hover:shadow-md hover:border-blue-200/50'
                      }`}
                      onClick={() => handleSearchItemClick(item.href)}
                    >
                      <div className="flex items-center space-x-4 w-full">
                        {item.icon && (
                          <div className="flex items-center justify-center w-10 h-10 rounded-xl bg-gradient-to-br from-blue-100 to-purple-100 group-hover:from-blue-200 group-hover:to-purple-200 transition-all duration-300">
                            <item.icon className="h-5 w-5 text-blue-600" />
                          </div>
                        )}
                        <div className="flex flex-col items-start flex-1 min-w-0">
                          <span className="font-semibold text-gray-800 group-hover:text-blue-800 transition-colors duration-200 truncate w-full text-left">
                            {item.title}
                          </span>
                          {item.description && (
                            <span className="text-sm text-gray-500 group-hover:text-gray-600 transition-colors duration-200 truncate w-full text-left">
                              {item.description}
                            </span>
                          )}
                        </div>
                        <div className="opacity-0 group-hover:opacity-100 transition-opacity duration-200">
                          <div className="p-1 rounded-lg bg-blue-100 text-blue-600">
                            <ChevronDown className="h-4 w-4 rotate-[-90deg]" />
                          </div>
                        </div>
                      </div>
                    </Button>
                  ))}
                </div>
              ) : searchQuery.trim() ? (
                <div className="p-12 text-center">
                  <div className="w-16 h-16 mx-auto mb-4 rounded-full bg-gradient-to-br from-gray-100 to-gray-200 flex items-center justify-center">
                    <Search className="h-8 w-8 text-gray-400" />
                  </div>
                  <h3 className="text-lg font-medium text-gray-700 mb-2">No results found</h3>
                  <p className="text-gray-500 mb-4">
                    We couldn't find anything matching "<span className="font-medium text-gray-700">{searchQuery}</span>"
                  </p>
                  <Button
                    variant="outline"
                    size="sm"
                    onClick={() => setSearchQuery('')}
                    className="rounded-xl border-gray-200 hover:bg-gray-50"
                  >
                    Clear search
                  </Button>
                </div>
              ) : (
                <div className="p-12 text-center">
                  <div className="w-20 h-20 mx-auto mb-6 rounded-full bg-gradient-to-br from-blue-100 via-purple-100 to-pink-100 flex items-center justify-center">
                    <Search className="h-10 w-10 text-blue-500" />
                  </div>
                  <h3 className="text-xl font-semibold text-gray-800 mb-3">Quick Search</h3>
                  <p className="text-gray-500 mb-6 max-w-sm mx-auto">
                    Start typing to search for pages, features, and more across the application
                  </p>
                  
                  {/* Popular shortcuts */}
                  <div className="space-y-2">
                    <p className="text-sm font-medium text-gray-600 mb-3">Popular shortcuts:</p>
                    <div className="flex flex-wrap justify-center gap-2">
                      {[
                        { label: 'Classes', href: '/admin/classes' },
                        { label: 'Students', href: '/admin/students' },
                        { label: 'Teachers', href: '/admin/teachers' },
                        { label: 'Settings', href: '/settings' }
                      ].map((shortcut, index) => (
                        <Button
                          key={index}
                          variant="outline"
                          size="sm"
                          onClick={() => handleSearchItemClick(shortcut.href)}
                          className="text-xs rounded-xl border-gray-200 hover:bg-blue-50 hover:border-blue-300 hover:text-blue-700 transition-all duration-200"
                        >
                          {shortcut.label}
                        </Button>
                      ))}
                    </div>
                  </div>
                </div>
              )}
            </ScrollArea>
          </div>
        </DialogContent>
      </Dialog>
    </>
  )
} 