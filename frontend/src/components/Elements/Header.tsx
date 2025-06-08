// src/components/Header.tsx
import { Link } from "react-router-dom"
import { Menu } from "lucide-react"
import { Button } from "@/components/ui/button"
import { Sheet, SheetContent, SheetTrigger } from "@/components/ui/sheet"
import { ScrollArea } from "@/components/ui/scroll-area"
import ModeToggle  from "@/components/mode-toggle"

export function Header() {
  return (
    <header className="border-b bg-background">
      <div className=" mx-auto flex h-16 items-center justify-between px-4">
        {/* Logo / Brand */}
        <Link to="/" className="text-2xl font-bold">
          MyApp
        </Link>

        {/* Desktop nav */}
        <nav className="hidden md:flex space-x-6">
          <Link
            to="/"
            className="text-sm font-medium text-muted-foreground hover:text-primary"
          >
            Home
          </Link>
          <Link
            to="/profile/me"
            className="text-sm font-medium text-muted-foreground hover:text-primary"
          >
            Profile
          </Link>
        </nav>

        <div className="flex items-center space-x-2">
          {/* Dark/Light mode toggle */}
          <ModeToggle />

          {/* Mobile menu */}
          <Sheet>
            <SheetTrigger asChild>
              <Button variant="ghost" size="icon" className="md:hidden">
                <Menu className="h-5 w-5" />
              </Button>
            </SheetTrigger>
            <SheetContent side="right" className="w-64 p-0">
              <ScrollArea className="h-full">
                <nav className="flex flex-col space-y-1 p-4">
                  <Link
                    to="/"
                    className="rounded-md px-3 py-2 text-base font-medium hover:bg-accent hover:text-accent-foreground"
                  >
                    Home
                  </Link>
                  <Link
                    to="/profile/me"
                    className="rounded-md px-3 py-2 text-base font-medium hover:bg-accent hover:text-accent-foreground"
                  >
                    Profile
                  </Link>
                </nav>
              </ScrollArea>
            </SheetContent>
          </Sheet>
        </div>
      </div>
    </header>
  )
}
