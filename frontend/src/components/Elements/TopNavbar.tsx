import React, { useState, useEffect, useCallback } from 'react'
import { Search, Settings, User, LogOut, ChevronDown, X, GraduationCap } from 'lucide-react'
import { NotificationBell } from '@/features/notifications/components/NotificationBell'
import { Button } from '@/components/ui/button'
import {
  DropdownMenu,
  DropdownMenuContent,
  DropdownMenuItem,
  DropdownMenuLabel,
  DropdownMenuTrigger,
} from '@/components/ui/dropdown-menu'
import {
  Dialog,
  DialogContent,
  DialogHeader,
  DialogTitle,
} from '@/components/ui/dialog'
import { Input } from '@/components/ui/input'

import { ScrollArea } from '@/components/ui/scroll-area'
import { useTranslation } from 'react-i18next'
import { useNavigate } from 'react-router-dom'
import { useDispatch } from 'react-redux'
import { logout, resetAuth } from '@/stores/authSlice'
import { performLogout } from '@/lib/auth'
import { useUserRole } from '@/hooks/useUserRole'
import { useAuth } from '@/hooks/useAuth'
import { menuConfig } from '@/config/menuConfig'
import { getRoleClasses } from '@/lib/theme'

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
  const { user } = useAuth()
  const roleClasses = getRoleClasses(userRole)
  const [isSearchOpen, setIsSearchOpen] = useState(false)
  const [searchQuery, setSearchQuery] = useState('')
  const [searchResults, setSearchResults] = useState<SearchItem[]>([])
  const [selectedIndex, setSelectedIndex] = useState(0)



  // Create searchable items from menu config
  const getAllSearchableItems = useCallback((): SearchItem[] => {
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
      { title: t('Settings'), description: 'User profile and settings', href: '/profile' },
      { title: t('Help'), description: 'Help and support', href: '/help' }
    )

    return items
  }, [userRole, t])

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
  }, [searchQuery, userRole, t, getAllSearchableItems])

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
        <header className="sticky top-0 z-50 bg-white/95 backdrop-blur-md border-b border-slate-200/60 shadow-sm mx-6 rounded-b-lg">
          {/* Main navbar content */}
          <div className="flex h-16 items-center justify-between px-6">
          
              {/* Center - Search */}
              <div className="flex-1 max-w-lg mx-4">
                <Button
                  variant="outline"
                  className="relative w-full justify-start text-sm text-slate-500 border-slate-200 bg-slate-50/50 hover:bg-white hover:border-slate-300 hover:shadow-md transition-all duration-200 rounded-xl group h-10"
                  onClick={() => setIsSearchOpen(true)}
                >
                  <Search className="mr-3 h-4 w-4 text-slate-400 group-hover:text-slate-600 transition-colors duration-200" />
                  <span className="text-slate-500 group-hover:text-slate-700 transition-colors duration-200">Search anything...</span>
                  <div className="ml-auto flex items-center">
                    <kbd className="pointer-events-none inline-flex h-5 select-none items-center gap-1 rounded border border-slate-200 bg-white px-1.5 font-mono text-[10px] font-medium text-slate-500 shadow-sm group-hover:border-slate-300 transition-all duration-200">
                      <span className="text-xs">⌘</span>K
                    </kbd>
                  </div>
                </Button>
              </div>

              {/* Right side - Actions */}
              <div className="flex items-center space-x-2">
                {/* Real-time Notifications */}
                <NotificationBell />

                {/* Learning Space Button */}
                <Button
                variant="ghost"
                onClick={() => navigate('/learning-space')}
                className={`flex items-center space-x-2 px-3 py-2 hover:shadow-sm transition-all duration-200 rounded-xl h-10
                  ${roleClasses.primaryLight} hover:${roleClasses.primaryBg} hover:text-white group`}
              >
                <div className={`w-7 h-7 ${roleClasses.primaryBg} group-hover:bg-white/20 rounded-lg flex items-center justify-center transition-colors duration-200`}>
                  <GraduationCap className="h-4 w-4 text-white" />
                </div>
                <span className={`hidden lg:block font-medium ${roleClasses.primary} group-hover:text-white transition-colors duration-200`}>
                  Learning Space
                </span>
              </Button>

              {/* User Profile */}
              <DropdownMenu>
                <DropdownMenuTrigger asChild>
                  <Button variant="ghost" className="flex items-center space-x-2 px-3 py-2 hover:bg-slate-100 hover:shadow-sm transition-all duration-200 rounded-xl h-10">
                    <div className="w-8 h-8 bg-gradient-to-br from-blue-500 to-indigo-600 rounded-full flex items-center justify-center shadow-md">
                      <User className="h-4 w-4 text-white" />
                    </div>
                    <span className="hidden lg:block font-medium text-slate-700 max-w-32 truncate">
                      {user ? `${user.firstName} ${user.lastName}` : 'User'}
                    </span>
                    <ChevronDown className="h-3 w-3 text-slate-400" />
                  </Button>
                </DropdownMenuTrigger>
                <DropdownMenuContent align="end" className="bg-white border-slate-200 shadow-xl rounded-xl w-64 p-2">
                  <DropdownMenuLabel className="px-0 py-0 mb-2">
                    <div className="flex items-center space-x-3 p-3 bg-gradient-to-r from-slate-50 to-blue-50/50 rounded-lg border border-slate-100">
                      <div className="relative">
                        <div className="w-10 h-10 bg-gradient-to-br from-blue-500 to-indigo-600 rounded-full flex items-center justify-center shadow-md">
                          <User className="h-5 w-5 text-white" />
                        </div>
                        <div className="absolute -bottom-0.5 -right-0.5 w-3 h-3 bg-green-500 rounded-full border border-white"></div>
                      </div>
                      <div className="flex flex-col min-w-0 flex-1">
                        <span className="font-bold text-slate-900 text-sm truncate">{user ? `${user.firstName} ${user.lastName}` : 'User'}</span>
                        <span className="text-xs text-slate-600 capitalize font-medium">
                          {user?.role?.toLowerCase() || 'User'}
                        </span>
                        <span className="text-xs text-slate-500 truncate">
                          {user?.email || 'user@example.com'}
                        </span>
                      </div>
                    </div>
                  </DropdownMenuLabel>
                  <div className="space-y-1">
                    <DropdownMenuItem 
                      onClick={() => navigate('/profile')} 
                      className="cursor-pointer text-slate-700 hover:text-white hover:bg-gradient-to-r hover:from-blue-500 hover:to-indigo-600 transition-all duration-200 rounded-xl px-4 py-3 font-medium flex items-center"
                    >
                      <div className="w-8 h-8 bg-blue-100 rounded-lg flex items-center justify-center mr-3">
                        <Settings className="h-4 w-4 text-blue-600" />
                      </div>
                      <span>Profile Settings</span>
                    </DropdownMenuItem>
                    <DropdownMenuItem 
                      onClick={handleLogout} 
                      className="cursor-pointer text-red-600 font-semibold hover:text-white hover:bg-gradient-to-r hover:from-red-500 hover:to-red-600 transition-all duration-200 rounded-xl px-4 py-3 flex items-center"
                    >
                      <div className="w-8 h-8 bg-red-100 rounded-lg flex items-center justify-center mr-3">
                        <LogOut className="h-4 w-4 text-red-600" />
                      </div>
                      <span>Sign out</span>
                    </DropdownMenuItem>
                  </div>
                </DropdownMenuContent>
                </DropdownMenu>
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