import * as React from "react";
import { useNavigate } from "react-router-dom";
import { toast } from "react-hot-toast";
import { ArrowLeft, Building, Info, Users, MapPin } from "lucide-react";
import { motion } from "framer-motion";

import { Button } from "@/components/ui/button";
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from "@/components/ui/card";
import { AutoForm } from "@/form/AutoForm";
import { roomFormDefinition, type RoomFormValues } from "@/features/rooms/roomForm.definition";
import { useCreateRoom } from "@/features/rooms/hooks/use-rooms";

export default function RoomsCreate() {
  const navigate = useNavigate();
  const createMutation = useCreateRoom();

  const handleSubmit = async (values: unknown) => {
    try {
      const roomValues = values as RoomFormValues;
      await createMutation.mutateAsync(roomValues);
      toast.success("Room created successfully");
      navigate("/admin/rooms");
    } catch (error) {
      console.error("Failed to create room:", error);
      toast.error("Failed to create room");
    }
  };

  const formDefinition = React.useMemo(() => ({
    ...roomFormDefinition,
    onSubmit: handleSubmit,
  }), [handleSubmit]);

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
          <div>
            <h1 className="text-3xl font-bold text-gray-900">Create New Room</h1>
            <p className="text-gray-600 mt-1">Add a new room to the school system</p>
          </div>
        </motion.div>

        <div className="grid grid-cols-1 lg:grid-cols-3 gap-8">
          {/* Form Section */}
          <motion.div
            initial={{ opacity: 0, x: -20 }}
            animate={{ opacity: 1, x: 0 }}
            transition={{ duration: 0.5, delay: 0.1 }}
            className="lg:col-span-2"
          >
            <Card className="shadow-lg border-0 bg-white/80 backdrop-blur-sm">
              <CardHeader className="bg-gradient-to-r from-blue-600 to-indigo-600 text-white rounded-t-lg">
                <CardTitle className="flex items-center gap-2">
                  <Building className="h-5 w-5" />
                  Room Information
                </CardTitle>
                <CardDescription className="text-blue-100">
                  Fill in the details for the new room
                </CardDescription>
              </CardHeader>
              <CardContent className="p-8">
                <AutoForm
                  recipe={formDefinition}
                  submitLabel="Create Room"
                />
              </CardContent>
            </Card>
          </motion.div>

          {/* Guidelines Section */}
          <motion.div
            initial={{ opacity: 0, x: 20 }}
            animate={{ opacity: 1, x: 0 }}
            transition={{ duration: 0.5, delay: 0.2 }}
            className="space-y-6"
          >
            <Card className="shadow-lg border-0 bg-white/80 backdrop-blur-sm">
              <CardHeader className="bg-gradient-to-r from-green-600 to-emerald-600 text-white rounded-t-lg">
                <CardTitle className="flex items-center gap-2">
                  <Info className="h-5 w-5" />
                  Room Guidelines
                </CardTitle>
              </CardHeader>
              <CardContent className="p-6">
                <div className="space-y-4">
                  <div className="flex items-start gap-3">
                    <Building className="h-5 w-5 text-blue-600 mt-0.5 flex-shrink-0" />
                    <div>
                      <h4 className="font-semibold text-gray-900">Room Name</h4>
                      <p className="text-sm text-gray-600">
                        Use a clear, unique identifier like "Room A101" or "Physics Lab"
                      </p>
                    </div>
                  </div>
                  <div className="flex items-start gap-3">
                    <Users className="h-5 w-5 text-green-600 mt-0.5 flex-shrink-0" />
                    <div>
                      <h4 className="font-semibold text-gray-900">Capacity</h4>
                      <p className="text-sm text-gray-600">
                        Enter the maximum number of people the room can safely accommodate
                      </p>
                    </div>
                  </div>
                  <div className="flex items-start gap-3">
                    <MapPin className="h-5 w-5 text-purple-600 mt-0.5 flex-shrink-0" />
                    <div>
                      <h4 className="font-semibold text-gray-900">Room Type</h4>
                      <p className="text-sm text-gray-600">
                        Select the appropriate category based on the room's primary function
                      </p>
                    </div>
                  </div>
                </div>
              </CardContent>
            </Card>

            <Card className="shadow-lg border-0 bg-white/80 backdrop-blur-sm">
              <CardHeader className="bg-gradient-to-r from-purple-600 to-violet-600 text-white rounded-t-lg">
                <CardTitle className="flex items-center gap-2">
                  <Building className="h-5 w-5" />
                  Room Types
                </CardTitle>
              </CardHeader>
              <CardContent className="p-6">
                <div className="space-y-3">
                  <div className="flex items-center gap-2">
                    <div className="w-2 h-2 bg-blue-500 rounded-full"></div>
                    <span className="text-sm font-medium">Classroom</span>
                    <span className="text-xs text-gray-500">- Regular teaching spaces</span>
                  </div>
                  <div className="flex items-center gap-2">
                    <div className="w-2 h-2 bg-purple-500 rounded-full"></div>
                    <span className="text-sm font-medium">Laboratory</span>
                    <span className="text-xs text-gray-500">- Science and research labs</span>
                  </div>
                  <div className="flex items-center gap-2">
                    <div className="w-2 h-2 bg-indigo-500 rounded-full"></div>
                    <span className="text-sm font-medium">Auditorium</span>
                    <span className="text-xs text-gray-500">- Large presentation spaces</span>
                  </div>
                  <div className="flex items-center gap-2">
                    <div className="w-2 h-2 bg-gray-500 rounded-full"></div>
                    <span className="text-sm font-medium">Office</span>
                    <span className="text-xs text-gray-500">- Administrative spaces</span>
                  </div>
                  <div className="flex items-center gap-2">
                    <div className="w-2 h-2 bg-orange-500 rounded-full"></div>
                    <span className="text-sm font-medium">Gymnasium</span>
                    <span className="text-xs text-gray-500">- Sports and fitness areas</span>
                  </div>
                </div>
              </CardContent>
            </Card>
          </motion.div>
        </div>
      </div>
    </div>
  );
} 