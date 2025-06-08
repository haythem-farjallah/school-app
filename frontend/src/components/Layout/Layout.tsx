import { Outlet } from 'react-router-dom';
import { AppSidebar } from '../Elements/AppSidebar';
import { TopNavbar } from '../Elements/TopNavbar';
import { SidebarProvider, SidebarInset } from '@/components/ui/sidebar';

export const Layout = () => {
  return (
    <div className="min-h-screen bg-gray-50">
      <SidebarProvider>
        <AppSidebar />
        <SidebarInset className="bg-gray-50">
          <TopNavbar />
          <main className="flex-1 overflow-auto p-6">
            <div className="bg-white rounded-lg border border-gray-200 shadow-sm min-h-[calc(100vh-140px)]">
              <div className="p-6">
                <Outlet />
              </div>
            </div>
          </main>
        </SidebarInset>
      </SidebarProvider>
    </div>
  );
}; 