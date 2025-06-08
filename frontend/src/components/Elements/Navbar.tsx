// src/components/Elements/Navbar.tsx
import { MessageSquare, Bell } from 'lucide-react'

export default function Navbar() {
  return (
    <header className="flex items-center justify-between px-6 py-4 bg-surface-0 dark:bg-surface-2 border-b border-border">
      {/* — Search input — */}
      <div className="w-1/3">
        <input
          type="text"
          placeholder="Search..."
          className="
            w-full px-4 py-2
            bg-surface-1 dark:bg-surface-3
            border border-border rounded-lg
            focus:outline-none focus:ring-2 focus:ring-sky
            text-text-lo
          "
        />
      </div>

      {/* — Icons + Profile — */}
      <div className="flex items-center gap-4">
        {/* Chat */}
        <button className="p-2 rounded-lg hover:bg-surface-1 dark:hover:bg-surface-3 transition-colors">
          <MessageSquare className="w-6 h-6 text-text-lo" />
        </button>

        {/* Notifications */}
        <button className="relative p-2 rounded-lg hover:bg-surface-1 dark:hover:bg-surface-3 transition-colors">
          <Bell className="w-6 h-6 text-text-lo" />
          <span className="
            absolute -top-1 -right-1
            inline-flex items-center justify-center
            w-4 h-4 text-xs font-medium
            text-white bg-error rounded-full
          ">
            1
          </span>
        </button>

        {/* Profile */}
        <div className="flex items-center gap-2 cursor-pointer">
          <img
            src="/avatar.png"
            alt="User Avatar"
            width={32}
            height={32}
            className="rounded-full"
          />
          <div className="hidden sm:flex flex-col leading-tight">
            <span className="text-text-hi font-medium">Adam Holland</span>
            <span className="text-text-lo text-sm">admin</span>
          </div>
        </div>
      </div>
    </header>
  )
}
