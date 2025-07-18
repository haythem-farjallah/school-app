import * as React from "react";
import { toast } from "react-hot-toast";
import {
  Sheet,
  SheetContent,
  SheetDescription,
  SheetHeader,
  SheetTitle,
  SheetTrigger,
} from "@/components/ui/sheet";
import { AutoForm } from "@/form/AutoForm";
import { roomFormDefinition, type RoomFormValues } from "../roomForm.definition";
import { useUpdateRoom } from "../hooks/use-rooms";
import type { Room } from "@/types/room";

interface EditRoomSheetProps {
  room: Room;
  onSuccess?: () => void;
  trigger: React.ReactNode;
}

export function EditRoomSheet({ room, onSuccess, trigger }: EditRoomSheetProps) {
  const [isOpen, setIsOpen] = React.useState(false);
  const updateMutation = useUpdateRoom();

  const handleSubmit = async (values: unknown) => {
    try {
      const roomValues = values as RoomFormValues;
      await updateMutation.mutateAsync({
        id: room.id,
        data: roomValues,
      });
      toast.success("Room updated successfully");
      setIsOpen(false);
      onSuccess?.();
    } catch (error) {
      console.error("Failed to update room:", error);
      toast.error("Failed to update room");
    }
  };

  const formDefinition = React.useMemo(() => ({
    ...roomFormDefinition,
    onSubmit: handleSubmit,
  }), [handleSubmit]);

  return (
    <Sheet open={isOpen} onOpenChange={setIsOpen}>
      <SheetTrigger asChild>
        {trigger}
      </SheetTrigger>
      <SheetContent className="sm:max-w-md">
        <SheetHeader>
          <SheetTitle>Edit Room</SheetTitle>
          <SheetDescription>
            Update the room information. Click save when you're done.
          </SheetDescription>
        </SheetHeader>
        <div className="mt-6">
          <AutoForm
            recipe={formDefinition}
            defaultValues={{
              name: room.name,
              capacity: room.capacity,
              roomType: room.roomType,
            }}
            submitLabel="Save Changes"
          />
        </div>
      </SheetContent>
    </Sheet>
  );
} 