import { z } from "zod";
import { FormRecipe } from "@/form/types";
import { RoomType } from "@/types/room";

export const roomFormSchema = z.object({
  name: z.string().min(1, "Room name is required").max(100, "Room name must be less than 100 characters"),
  capacity: z.number().min(1, "Capacity must be at least 1").max(1000, "Capacity must be less than 1000"),
  roomType: z.nativeEnum(RoomType, { errorMap: () => ({ message: "Please select a valid room type" }) }),
});

export type RoomFormValues = z.infer<typeof roomFormSchema>;

export const roomFormDefinition: FormRecipe = {
  schema: roomFormSchema,
  fields: [
    {
      name: "name",
      label: "Room Name",
      type: "text",
      placeholder: "Enter room name (e.g., Room A101, Physics Lab)",
      description: "A unique identifier for the room",
      required: true,
    },
    {
      name: "capacity",
      label: "Capacity",
      type: "number",
      placeholder: "Enter room capacity",
      description: "Maximum number of people the room can accommodate",
      required: true,
    },
    {
      name: "roomType",
      label: "Room Type",
      type: "select",
      placeholder: "Select room type",
      description: "The type/category of the room",
      required: true,
      options: [
        { value: RoomType.CLASSROOM, label: "Classroom" },
        { value: RoomType.LABORATORY, label: "Laboratory" },
        { value: RoomType.AUDITORIUM, label: "Auditorium" },
        { value: RoomType.OFFICE, label: "Office" },
        { value: RoomType.GYM, label: "Gymnasium" },
      ],
    },
  ],
  onSubmit: () => {
    throw new Error("onSubmit must be implemented by the component using this form");
  },
}; 