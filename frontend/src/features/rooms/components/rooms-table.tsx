import * as React from "react";
import toast from "react-hot-toast";
import { useNavigate } from "react-router-dom";
import { type Parser, useQueryState, useQueryStates, parseAsInteger, parseAsString } from "nuqs";
import { useUserRole } from "@/hooks/useUserRole";

import { useDataTable } from "@/hooks/use-data-table";
import { DataTable } from "@/components/data-table/data-table";
import { DataTableToolbar } from "@/components/data-table/data-table-toolbar";
import { DataTableSkeleton } from "@/components/data-table/data-table-skeleton";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";
import { Button } from "@/components/ui/button";
import { Building, MapPin, Users, Plus } from "lucide-react";

import { useRooms, useDeleteRoom } from "../hooks/use-rooms";
import { getRoomsColumns } from "./room-columns";
import type { Room } from "@/types/room";

export function RoomsTable() {
  const navigate = useNavigate();
  const userRole = useUserRole();
  const deleteMutation = useDeleteRoom();
  
  // Dynamic base path based on user role
  const basePath = userRole?.toLowerCase() === 'staff' ? '/staff' : '/admin';

  // URL pagination parameters (managed by useDataTable)
  const [urlPage] = useQueryState("page", parseAsInteger.withDefault(1));
  const [pageSize] = useQueryState("perPage", parseAsInteger.withDefault(10));
  const currentPageIndex = urlPage - 1;

  // Build filter parsers dynamically - we'll define columns later
  const filterParsers = React.useMemo(() => {
    const parsers: Record<string, Parser<string>> = {};
    // Define the parsers based on known filterable columns
    const filterableColumns = [
      { key: "name", enableColumnFilter: true },
      { key: "capacity", enableColumnFilter: true },
      { key: "roomType", enableColumnFilter: true },
    ];
    
    filterableColumns.forEach((col) => {
      if (col.enableColumnFilter) {
        parsers[col.key] = parseAsString.withDefault("").withOptions({ shallow: true });
      }
    });
    return parsers;
  }, []);

  const [filterValues] = useQueryStates(filterParsers);

  // Map frontend column keys to backend filter parameter names
  const apiParams = React.useMemo(() => {
    const keyMap: Record<string, string> = {
      name: "name",
      capacity: "minCapacity",
      roomType: "roomType",
    };

    const params: Record<string, unknown> = {};

    Object.entries(filterValues).forEach(([key, val]) => {
      if (typeof val === "string" && val.trim()) {
        const backendKey = keyMap[key] ?? key;
        params[backendKey] = val.trim();
      }
    });

    return params;
  }, [filterValues]);

  const { data: roomsResponse, isLoading, error, refetch } = useRooms({
    page: currentPageIndex,
    size: pageSize,
    ...apiParams,
  });

  const rooms = roomsResponse?.data ?? [];
  const totalItems = roomsResponse?.totalItems ?? 0;
  const totalPages = Math.ceil(totalItems / pageSize);

  const handleView = React.useCallback((room: Room) => {
    console.log("👁️ RoomsTable - Viewing room:", room);
    navigate(`${basePath}/rooms/view/${room.id}`);
  }, [navigate, basePath]);

  const handleEdit = React.useCallback((room: Room) => {
    console.log("✏️ RoomsTable - Editing room:", room);
    // Edit logic is now handled by the EditRoomSheet component
  }, []);

  const handleDelete = React.useCallback((room: Room) => {
    console.log("🗑️ RoomsTable - Deleting room:", room);
    if (window.confirm(`Are you sure you want to delete the room "${room.name}"?`)) {
      deleteMutation.mutate(room.id, {
        onSuccess: () => {
          toast.success("Room deleted successfully");
          refetch();
        },
        onError: (error) => {
          console.error("❌ RoomsTable - Delete error:", error);
          toast.error("Failed to delete room");
        },
      });
    }
  }, [deleteMutation, refetch]);

  const handleCreate = React.useCallback(() => {
    console.log("➕ RoomsTable - Creating new room");
    navigate(`${basePath}/rooms/create`);
  }, [navigate, basePath]);

  const columns = React.useMemo(
    () => getRoomsColumns({
      onView: handleView,
      onEdit: handleEdit,
      onDelete: handleDelete,
      onSuccess: () => refetch(),
    }),
    [handleView, handleEdit, handleDelete, refetch]
  );

  const { table } = useDataTable({
    data: rooms,
    columns,
    pageCount: totalPages,
    initialState: {
      pagination: {
        pageIndex: currentPageIndex,
        pageSize,
      },
    },
  });

  if (isLoading) {
    return <DataTableSkeleton columnCount={4} rowCount={10} />;
  }

  if (error) {
    return (
      <div className="text-center py-8">
        <p className="text-red-500">Error loading rooms: {error.message}</p>
        <Button onClick={() => refetch()} variant="outline" className="mt-2">
          Try Again
        </Button>
      </div>
    );
  }

  return (
    <div className="space-y-6">
      {/* Header with stats cards */}
      <div className="flex items-center justify-between">
        <div>
          <h2 className="text-2xl font-bold tracking-tight text-gray-900">Rooms</h2>
          <p className="text-gray-600">Manage school rooms and facilities</p>
        </div>
        <Button onClick={handleCreate} className="bg-blue-600 hover:bg-blue-700">
          <Plus className="mr-2 h-4 w-4" />
          Add Room
        </Button>
      </div>

      {/* Stats Cards */}
      <div className="grid grid-cols-1 md:grid-cols-3 gap-6">
        <Card className="border-blue-200 bg-gradient-to-br from-blue-50 to-indigo-50">
          <CardHeader className="flex flex-row items-center justify-between space-y-0 pb-2">
            <CardTitle className="text-sm font-medium text-blue-800">Total Rooms</CardTitle>
            <Building className="h-4 w-4 text-blue-600" />
          </CardHeader>
          <CardContent>
            <div className="text-3xl font-bold text-blue-700">{totalItems}</div>
            <p className="text-blue-600/70 text-sm mt-1">Active rooms in system</p>
          </CardContent>
        </Card>

        <Card className="border-green-200 bg-gradient-to-br from-green-50 to-emerald-50">
          <CardHeader className="flex flex-row items-center justify-between space-y-0 pb-2">
            <CardTitle className="text-sm font-medium text-green-800">Available Rooms</CardTitle>
            <MapPin className="h-4 w-4 text-green-600" />
          </CardHeader>
          <CardContent>
            <div className="text-3xl font-bold text-green-700">{rooms.length}</div>
            <p className="text-green-600/70 text-sm mt-1">Ready for scheduling</p>
          </CardContent>
        </Card>

        <Card className="border-purple-200 bg-gradient-to-br from-purple-50 to-violet-50">
          <CardHeader className="flex flex-row items-center justify-between space-y-0 pb-2">
            <CardTitle className="text-sm font-medium text-purple-800">Total Capacity</CardTitle>
            <Users className="h-4 w-4 text-purple-600" />
          </CardHeader>
          <CardContent>
            <div className="text-3xl font-bold text-purple-700">
              {rooms.reduce((sum, room) => sum + room.capacity, 0)}
            </div>
            <p className="text-purple-600/70 text-sm mt-1">People across all rooms</p>
          </CardContent>
        </Card>
      </div>

      {/* Data Table */}
      <Card className="border-gray-200 shadow-sm">
        <CardContent className="p-0">
          <DataTableToolbar table={table} />
          <DataTable table={table} />
        </CardContent>
      </Card>
    </div>
  );
} 