import * as React from "react";
import { useNavigate, useParams } from "react-router-dom";
import { toast } from "react-hot-toast";
import { ArrowLeft, Building, Users, MapPin, Edit, Trash2 } from "lucide-react";
import { motion } from "framer-motion";

import { Button } from "@/components/ui/button";
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from "@/components/ui/card";
import { Badge } from "@/components/ui/badge";
import { Skeleton } from "@/components/ui/skeleton";
import { useRoom, useDeleteRoom } from "@/features/rooms/hooks/use-rooms";
import { EditRoomSheet } from "@/features/rooms/components/room-sheet";
import { RoomType } from "@/types/room";

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

export default function RoomsView() {
  const { id } = useParams<{ id: string }>();
  const navigate = useNavigate();
  const deleteMutation = useDeleteRoom();

  const { data: room, isLoading, error, refetch } = useRoom(id ? parseInt(id) : undefined);

  const handleDelete = async () => {
    if (!room) return;
    
    if (window.confirm(`Are you sure you want to delete the room "${room.name}"?`)) {
      try {
        await deleteMutation.mutateAsync(room.id);
        toast.success("Room deleted successfully");
        navigate("/admin/rooms");
      } catch (error) {
        console.error("Failed to delete room:", error);
        toast.error("Failed to delete room");
      }
    }
  };

  if (isLoading) {
    return (
      <div className="min-h-screen bg-gradient-to-br from-blue-50 via-white to-indigo-50">
        <div className="container mx-auto px-4 py-8">
          <div className="flex items-center gap-4 mb-8">
            <Skeleton className="h-8 w-32" />
            <Skeleton className="h-6 w-px" />
            <div>
              <Skeleton className="h-8 w-48 mb-2" />
              <Skeleton className="h-4 w-32" />
            </div>
          </div>
          <div className="grid grid-cols-1 lg:grid-cols-3 gap-8">
            <div className="lg:col-span-2 space-y-6">
              <Skeleton className="h-48 w-full" />
              <Skeleton className="h-32 w-full" />
            </div>
            <div className="space-y-6">
              <Skeleton className="h-24 w-full" />
              <Skeleton className="h-32 w-full" />
            </div>
          </div>
        </div>
      </div>
    );
  }

  if (error || !room) {
    return (
      <div className="min-h-screen bg-gradient-to-br from-blue-50 via-white to-indigo-50">
        <div className="container mx-auto px-4 py-8">
          <div className="text-center py-12">
            <h2 className="text-2xl font-bold text-gray-900 mb-4">Room Not Found</h2>
            <p className="text-gray-600 mb-6">
              The room you're looking for doesn't exist or has been removed.
            </p>
            <Button onClick={() => navigate("/admin/rooms")} variant="outline">
              Back to Rooms
            </Button>
          </div>
        </div>
      </div>
    );
  }

  return (
    <div className="min-h-screen bg-gradient-to-br from-blue-50 via-white to-indigo-50">
      <div className="container mx-auto px-4 py-8">
        {/* Header */}
        <motion.div
          initial={{ opacity: 0, y: -20 }}
          animate={{ opacity: 1, y: 0 }}
          transition={{ duration: 0.5 }}
          className="flex items-center gap-4 mb-8"
        >
          <Button
            variant="ghost"
            size="sm"
            onClick={() => navigate("/admin/rooms")}
            className="flex items-center gap-2 text-gray-600 hover:text-gray-900 hover:bg-gray-100 transition-colors"
          >
            <ArrowLeft className="h-4 w-4" />
            Back to Rooms
          </Button>
          <div className="h-6 w-px bg-gray-300" />
          <div className="flex-1">
            <h1 className="text-3xl font-bold text-gray-900">{room.name}</h1>
            <p className="text-gray-600 mt-1">Room details and information</p>
          </div>
          <div className="flex items-center gap-2">
            <EditRoomSheet
              room={room}
              onSuccess={() => refetch()}
              trigger={
                <Button variant="outline" size="sm" className="flex items-center gap-2">
                  <Edit className="h-4 w-4" />
                  Edit
                </Button>
              }
            />
            <Button
              variant="outline"
              size="sm"
              onClick={handleDelete}
              className="flex items-center gap-2 text-red-600 hover:text-red-700 hover:bg-red-50"
            >
              <Trash2 className="h-4 w-4" />
              Delete
            </Button>
          </div>
        </motion.div>

        <div className="grid grid-cols-1 lg:grid-cols-3 gap-8">
          {/* Main Content */}
          <motion.div
            initial={{ opacity: 0, x: -20 }}
            animate={{ opacity: 1, x: 0 }}
            transition={{ duration: 0.5, delay: 0.1 }}
            className="lg:col-span-2 space-y-6"
          >
            {/* Room Information */}
            <Card className="shadow-lg border-0 bg-white/80 backdrop-blur-sm">
              <CardHeader className="bg-gradient-to-r from-blue-600 to-indigo-600 text-white rounded-t-lg">
                <CardTitle className="flex items-center gap-2">
                  <Building className="h-5 w-5" />
                  Room Information
                </CardTitle>
                <CardDescription className="text-blue-100">
                  Basic details about this room
                </CardDescription>
              </CardHeader>
              <CardContent className="p-8">
                <div className="grid grid-cols-1 md:grid-cols-2 gap-6">
                  <div className="space-y-4">
                    <div>
                      <label className="text-sm font-medium text-gray-600">Room Name</label>
                      <p className="text-lg font-semibold text-gray-900 mt-1">{room.name}</p>
                    </div>
                    <div>
                      <label className="text-sm font-medium text-gray-600">Room Type</label>
                      <div className="mt-2">
                        <Badge className={`${getRoomTypeColor(room.roomType)} font-medium px-3 py-1 text-sm rounded-full border-0`}>
                          {getRoomTypeLabel(room.roomType)}
                        </Badge>
                      </div>
                    </div>
                  </div>
                  <div className="space-y-4">
                    <div>
                      <label className="text-sm font-medium text-gray-600">Capacity</label>
                      <div className="flex items-center gap-2 mt-1">
                        <Users className="h-5 w-5 text-gray-400" />
                        <span className="text-lg font-semibold text-gray-900">{room.capacity}</span>
                        <span className="text-gray-500">people</span>
                      </div>
                    </div>
                    <div>
                      <label className="text-sm font-medium text-gray-600">Room ID</label>
                      <p className="text-lg font-semibold text-gray-900 mt-1">#{room.id}</p>
                    </div>
                  </div>
                </div>
              </CardContent>
            </Card>
          </motion.div>

          {/* Sidebar */}
          <motion.div
            initial={{ opacity: 0, x: 20 }}
            animate={{ opacity: 1, x: 0 }}
            transition={{ duration: 0.5, delay: 0.2 }}
            className="space-y-6"
          >
            {/* Quick Stats */}
            <Card className="shadow-lg border-0 bg-white/80 backdrop-blur-sm">
              <CardHeader className="bg-gradient-to-r from-green-600 to-emerald-600 text-white rounded-t-lg">
                <CardTitle className="flex items-center gap-2">
                  <MapPin className="h-5 w-5" />
                  Quick Stats
                </CardTitle>
              </CardHeader>
              <CardContent className="p-6">
                <div className="space-y-4">
                  <div className="flex items-center justify-between">
                    <span className="text-sm text-gray-600">Capacity</span>
                    <span className="font-semibold text-gray-900">{room.capacity} people</span>
                  </div>
                  <div className="flex items-center justify-between">
                    <span className="text-sm text-gray-600">Type</span>
                    <Badge className={`${getRoomTypeColor(room.roomType)} text-xs`}>
                      {getRoomTypeLabel(room.roomType)}
                    </Badge>
                  </div>
                  <div className="flex items-center justify-between">
                    <span className="text-sm text-gray-600">Room ID</span>
                    <span className="font-semibold text-gray-900">#{room.id}</span>
                  </div>
                </div>
              </CardContent>
            </Card>

            {/* Actions */}
            <Card className="shadow-lg border-0 bg-white/80 backdrop-blur-sm">
              <CardHeader className="bg-gradient-to-r from-purple-600 to-violet-600 text-white rounded-t-lg">
                <CardTitle>Actions</CardTitle>
              </CardHeader>
              <CardContent className="p-6">
                <div className="space-y-3">
                  <EditRoomSheet
                    room={room}
                    onSuccess={() => refetch()}
                    trigger={
                      <Button variant="outline" className="w-full justify-start">
                        <Edit className="mr-2 h-4 w-4" />
                        Edit Room
                      </Button>
                    }
                  />
                  <Button
                    variant="outline"
                    className="w-full justify-start text-red-600 hover:text-red-700 hover:bg-red-50"
                    onClick={handleDelete}
                  >
                    <Trash2 className="mr-2 h-4 w-4" />
                    Delete Room
                  </Button>
                </div>
              </CardContent>
            </Card>
          </motion.div>
        </div>
      </div>
    </div>
  );
} 