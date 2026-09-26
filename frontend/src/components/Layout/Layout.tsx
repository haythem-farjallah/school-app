import type { CSSProperties } from 'react';
import { Outlet } from 'react-router-dom';
import { AppSidebar } from '../Elements/AppSidebar';
import { TopNavbar } from '../Elements/TopNavbar';
import { SidebarProvider } from '@/components/ui/sidebar';

// Width of the collapsed (icon-only) desktop sidebar.
const shellStyle = { '--sidebar-width-icon': '3.5rem' } as CSSProperties;

export const Layout = () => {
  return (
    <SidebarProvider style={shellStyle}>
      <AppSidebar />
      <div className="flex min-w-0 flex-1 flex-col">
        <TopNavbar />
        <main className="min-w-0 flex-1 px-4 pb-8 pt-4 sm:px-6 lg:px-8 lg:pt-6">
          <div className="w-full">
            <Outlet />
          </div>
        </main>
      </div>
    </SidebarProvider>
  );
};
