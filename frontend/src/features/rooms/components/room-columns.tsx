import { type ColumnDef } from "@tanstack/react-table";

import { Badge } from "@/components/ui/badge";
import { DataTableColumnHeader } from "@/components/data-table/data-table-column-header";
import { Checkbox } from "@/components/ui/checkbox";
import type { Room } from "@/types/room";
import { RoomType } from "@/types/room";
import { RoomActions } from "./room-actions";
import { Link } from "react-router-dom";

const getRoomTypeColor = (roomType: RoomType) => {
  switch (roomType) {
    case RoomType.CLASSROOM:
      return 'bg-blue-100 text-blue-800';
    case RoomType.LABORATORY:
      return 'bg-purple-100 text-purple-800';
    case RoomType.AUDITORIUM:
      return 'bg-indigo-100 text-indigo-800';
    case RoomType.OFFICE:
      return 'bg-gray-100 text-gray-800';
    case RoomType.GYM:
      return 'bg-orange-100 text-orange-800';
    default:
      return 'bg-neutral-100 text-neutral-800';
  }
};

const getRoomTypeLabel = (roomType: RoomType) => {
  switch (roomType) {
    case RoomType.CLASSROOM:
      return 'Classroom';
    case RoomType.LABORATORY:
      return 'Laboratory';
    case RoomType.AUDITORIUM:
      return 'Auditorium';
    case RoomType.OFFICE:
      return 'Office';
    case RoomType.GYM:
      return 'Gymnasium';
    default:
      return roomType;
  }
};

export function getRoomsColumns(actions?: {
  onView?: (room: Room) => void;
  onEdit?: (room: Room) => void;
  onDelete?: (room: Room) => void;
  onSuccess?: () => void;
}): ColumnDef<Room>[] {
  return [
    {
      id: "select",
      header: ({ table }) => (
        <Checkbox
          checked={
            table.getIsAllPageRowsSelected() ||
            (table.getIsSomePageRowsSelected() && "indeterminate")
          }
          onCheckedChange={(value) => table.toggleAllPageRowsSelected(!!value)}
          aria-label="Select all"
          className="translate-y-[2px] border-blue-300/80 data-[state=checked]:bg-gradient-to-r data-[state=checked]:from-blue-600 data-[state=checked]:to-indigo-600 transition-all duration-200"
        />
      ),
      cell: ({ row }) => (
        <Checkbox
          checked={row.getIsSelected()}
          onCheckedChange={(value) => row.toggleSelected(!!value)}
          aria-label="Select row"
          className="translate-y-[2px] border-blue-300/80 data-[state=checked]:bg-gradient-to-r data-[state=checked]:from-blue-600 data-[state=checked]:to-indigo-600 transition-all duration-200"
        />
      ),
      enableSorting: false,
      enableHiding: false,
      size: 40,
    },
    {
      accessorKey: "name",
      id: "name",
      header: ({ column }) => (
        <DataTableColumnHeader column={column} title="Room Name" />
      ),
      cell: ({ row }) => {
        const room = row.original;
        return (
          <div className="flex items-center space-x-2">
            <Link 
              to={`/admin/rooms/view/${room.id}`}
              className="font-medium text-blue-600 hover:text-blue-800 hover:underline transition-colors"
            >
              {room.name}
            </Link>
          </div>
        );
      },
      enableColumnFilter: true,
      filterFn: "includesString",
      meta: {
        variant: "text",
        label: "Room Name",
        placeholder: "Search rooms...",
      },
      size: 200,
    },
    {
      accessorKey: "capacity",
      id: "capacity",
      header: ({ column }) => (
        <DataTableColumnHeader column={column} title="Capacity" />
      ),
      cell: ({ row }) => {
        const capacity = row.getValue("capacity") as number;
        return (
          <div className="flex items-center">
            <span className="font-medium text-gray-900">{capacity}</span>
            <span className="text-gray-500 text-sm ml-1">people</span>
          </div>
        );
      },
      enableColumnFilter: true,
      filterFn: "includesString",
      meta: {
        variant: "number",
        label: "Capacity",
        placeholder: "Min capacity...",
      },
      size: 120,
    },
    {
      accessorKey: "roomType",
      id: "roomType",
      header: ({ column }) => (
        <DataTableColumnHeader column={column} title="Room Type" />
      ),
      cell: ({ row }) => {
        const roomType = row.getValue("roomType") as RoomType;
        return (
          <Badge 
            className={`${getRoomTypeColor(roomType)} font-medium px-2 py-1 text-xs rounded-full border-0`}
          >
            {getRoomTypeLabel(roomType)}
          </Badge>
        );
      },
      enableColumnFilter: true,
      filterFn: "includesString",
      meta: {
        variant: "select",
        label: "Room Type",
        options: [
          { label: "Classroom", value: RoomType.CLASSROOM },
          { label: "Laboratory", value: RoomType.LABORATORY },
          { label: "Auditorium", value: RoomType.AUDITORIUM },
          { label: "Office", value: RoomType.OFFICE },
          { label: "Gymnasium", value: RoomType.GYM },
        ],
      },
      size: 150,
    },
    {
      id: "actions",
      header: "Actions",
      cell: ({ row }) => (
        <div className="flex justify-end">
          <RoomActions 
            room={row.original} 
            onView={actions?.onView}
            onEdit={actions?.onEdit}
            onDelete={actions?.onDelete}
            onSuccess={actions?.onSuccess}
          />
        </div>
      ),
      enableSorting: false,
      enableHiding: false,
      size: 80,
    },
  ];
} 