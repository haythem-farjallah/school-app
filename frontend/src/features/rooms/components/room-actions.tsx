import { MoreHorizontal, Edit, Eye, Trash2 } from "lucide-react";

import { Button } from "@/components/ui/button";
import {
  DropdownMenu,
  DropdownMenuContent,
  DropdownMenuItem,
  DropdownMenuSeparator,
  DropdownMenuTrigger,
} from "@/components/ui/dropdown-menu";
import { EditRoomSheet } from "./room-sheet";
import type { Room } from "@/types/room";
import { useNavigate } from "react-router-dom";

interface RoomActionsProps {
  room: Room;
  onView?: (room: Room) => void;
  onEdit?: (room: Room) => void;
  onDelete?: (room: Room) => void;
  onSuccess?: () => void;
}

export function RoomActions({ room, onView, onDelete, onSuccess }: RoomActionsProps) {
  const navigate = useNavigate();
  
  return (
    <DropdownMenu>
      <DropdownMenuTrigger asChild>
        <Button
          variant="ghost"
          size="sm"
          className="h-8 w-8 p-0 rounded-full hover:bg-gradient-to-r hover:from-blue-50 hover:to-indigo-50 transition-colors duration-200 border border-transparent hover:border-blue-200/60"
        >
          <span className="sr-only">Open menu</span>
          <MoreHorizontal className="h-4 w-4 text-slate-600 hover:text-blue-600 transition-colors duration-200" />
        </Button>
      </DropdownMenuTrigger>
      <DropdownMenuContent align="end" className="w-48 bg-white/95 backdrop-blur-sm border border-slate-200/80 shadow-xl">
        <DropdownMenuItem 
          onClick={() => {
            if (onView) {
              onView(room);
            } else {
              navigate(`/admin/rooms/view/${room.id}`);
            }
          }}
          className="cursor-pointer hover:bg-gradient-to-r hover:from-blue-50 hover:to-indigo-50 transition-all duration-200 focus:bg-gradient-to-r focus:from-blue-50 focus:to-indigo-50"
        >
          <Eye className="mr-3 h-4 w-4 text-blue-600" />
          <span className="font-medium">View Details</span>
        </DropdownMenuItem>
        <EditRoomSheet 
          room={room} 
          onSuccess={onSuccess}
          trigger={
            <DropdownMenuItem 
              onSelect={(e) => e.preventDefault()}
              className="cursor-pointer hover:bg-gradient-to-r hover:from-green-50 hover:to-emerald-50 transition-all duration-200 focus:bg-gradient-to-r focus:from-green-50 focus:to-emerald-50"
            >
              <Edit className="mr-3 h-4 w-4 text-green-600" />
              <span className="font-medium">Edit Room</span>
            </DropdownMenuItem>
          }
        />
        <DropdownMenuSeparator className="bg-slate-200/60" />
        <DropdownMenuItem 
          onClick={() => onDelete?.(room)}
          className="cursor-pointer hover:bg-gradient-to-r hover:from-red-50 hover:to-rose-50 transition-all duration-200 focus:bg-gradient-to-r focus:from-red-50 focus:to-rose-50 text-red-600"
        >
          <Trash2 className="mr-3 h-4 w-4" />
          <span className="font-medium">Delete Room</span>
        </DropdownMenuItem>
      </DropdownMenuContent>
    </DropdownMenu>
  );
} 