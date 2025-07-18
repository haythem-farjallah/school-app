import type { ColumnDef } from "@tanstack/react-table";
import { MoreHorizontal, Edit, Trash2, Eye, EyeOff, Download, ExternalLink, FileText, Video, Image, Volume2, Presentation, Link as LinkIcon } from "lucide-react";

import { Badge } from "@/components/ui/badge";
import { Button } from "@/components/ui/button";
import { Checkbox } from "@/components/ui/checkbox";
import {
  DropdownMenu,
  DropdownMenuContent,
  DropdownMenuItem,
  DropdownMenuSeparator,
  DropdownMenuTrigger,
} from "@/components/ui/dropdown-menu";
import { DataTableColumnHeader } from "@/components/data-table/data-table-column-header";
import type { LearningResource } from "@/types/learning-resource";
import { ResourceType } from "@/types/learning-resource";
import { EditLearningResourceSheet } from "./learning-resource-sheet";

// Resource type icon mapping
const getResourceTypeIcon = (type: ResourceType) => {
  switch (type) {
    case ResourceType.VIDEO:
      return Video;
    case ResourceType.DOCUMENT:
      return FileText;
    case ResourceType.PRESENTATION:
      return Presentation;
    case ResourceType.AUDIO:
      return Volume2;
    case ResourceType.IMAGE:
      return Image;
    case ResourceType.LINK:
      return LinkIcon;
    default:
      return FileText;
  }
};

// Resource type color mapping
const getResourceTypeColor = (type: ResourceType) => {
  switch (type) {
    case ResourceType.VIDEO:
      return "bg-gradient-to-r from-red-100 to-pink-100 text-red-800 border-red-200/60";
    case ResourceType.DOCUMENT:
      return "bg-gradient-to-r from-blue-100 to-indigo-100 text-blue-800 border-blue-200/60";
    case ResourceType.PRESENTATION:
      return "bg-gradient-to-r from-orange-100 to-yellow-100 text-orange-800 border-orange-200/60";
    case ResourceType.AUDIO:
      return "bg-gradient-to-r from-purple-100 to-pink-100 text-purple-800 border-purple-200/60";
    case ResourceType.IMAGE:
      return "bg-gradient-to-r from-green-100 to-emerald-100 text-green-800 border-green-200/60";
    case ResourceType.LINK:
      return "bg-gradient-to-r from-cyan-100 to-blue-100 text-cyan-800 border-cyan-200/60";
    default:
      return "bg-gradient-to-r from-gray-100 to-slate-100 text-gray-800 border-gray-200/60";
  }
};

export function getLearningResourceColumns(actions?: {
  onView?: (resource: LearningResource) => void;
  onEdit?: (resource: LearningResource) => void;
  onDelete?: (resource: LearningResource) => void;
  onDownload?: (resource: LearningResource) => void;
  onSuccess?: () => void;
}): ColumnDef<LearningResource>[] {
  return [
    {
      id: "select",
      header: ({ table }) => (
        <Checkbox
          checked={table.getIsAllPageRowsSelected()}
          onCheckedChange={(value) => table.toggleAllPageRowsSelected(!!value)}
          aria-label="Select all"
          className="translate-y-[2px]"
        />
      ),
      cell: ({ row }) => (
        <Checkbox
          checked={row.getIsSelected()}
          onCheckedChange={(value) => row.toggleSelected(!!value)}
          aria-label="Select row"
          className="translate-y-[2px]"
        />
      ),
      enableSorting: false,
      enableHiding: false,
      size: 40,
    },
    {
      accessorKey: "title",
      header: ({ column }) => (
        <DataTableColumnHeader column={column} title="Title" />
      ),
      cell: ({ row }) => {
        const resource = row.original;
        const IconComponent = getResourceTypeIcon(resource.type);
        
        return (
          <div className="flex items-center space-x-3">
            <div className="h-10 w-10 rounded-full bg-gradient-to-r from-indigo-100 to-purple-100 border border-indigo-200/60 flex items-center justify-center">
              <IconComponent className="h-5 w-5 text-indigo-600" />
            </div>
            <div>
              <div className="font-semibold text-slate-900">{resource.title}</div>
              <div className="text-sm text-slate-500 truncate max-w-[200px]">
                {resource.description}
              </div>
            </div>
          </div>
        );
      },
      enableColumnFilter: true,
      meta: {
        variant: "text",
        label: "Title",
      },
      size: 300,
    },
    {
      accessorKey: "type",
      header: ({ column }) => (
        <DataTableColumnHeader column={column} title="Type" />
      ),
      cell: ({ row }) => {
        const type = row.getValue("type") as ResourceType;
        const IconComponent = getResourceTypeIcon(type);
        const colorClasses = getResourceTypeColor(type);
        
        return (
          <div className="flex items-center space-x-2">
            <IconComponent className="h-4 w-4" />
            <Badge 
              variant="outline" 
              className={`${colorClasses} font-semibold transition-colors duration-200`}
            >
              {type}
            </Badge>
          </div>
        );
      },
      enableColumnFilter: true,
      meta: {
        variant: "text",
        label: "Resource Type",
      },
      size: 150,
    },
    {
      accessorKey: "isPublic",
      header: ({ column }) => (
        <DataTableColumnHeader column={column} title="Visibility" />
      ),
      cell: ({ row }) => {
        const isPublic = row.getValue("isPublic") as boolean;
        
        return (
          <div className="flex items-center space-x-2">
            {isPublic ? (
              <Eye className="h-4 w-4 text-green-600" />
            ) : (
              <EyeOff className="h-4 w-4 text-orange-600" />
            )}
            <Badge 
              variant={isPublic ? "default" : "secondary"}
              className={
                isPublic 
                  ? "bg-gradient-to-r from-green-100 to-emerald-100 text-green-800 border border-green-200/60"
                  : "bg-gradient-to-r from-orange-100 to-yellow-100 text-orange-800 border border-orange-200/60"
              }
            >
              {isPublic ? "Public" : "Private"}
            </Badge>
          </div>
        );
      },
      enableColumnFilter: true,
      meta: {
        variant: "boolean",
        label: "Visibility",
      },
      size: 120,
    },
    {
      accessorKey: "duration",
      header: ({ column }) => (
        <DataTableColumnHeader column={column} title="Duration" />
      ),
      cell: ({ row }) => {
        const duration = row.getValue("duration") as number | undefined;
        
        if (!duration) return <span className="text-slate-400">-</span>;
        
        return (
          <Badge 
            variant="outline" 
            className="bg-gradient-to-r from-blue-100 to-indigo-100 text-blue-800 border border-blue-200/60 font-semibold"
          >
            {duration}min
          </Badge>
        );
      },
      enableColumnFilter: true,
      meta: {
        variant: "range",
        label: "Duration (minutes)",
      },
      size: 100,
    },
    {
      accessorKey: "url",
      header: ({ column }) => (
        <DataTableColumnHeader column={column} title="Link" />
      ),
      cell: ({ row }) => {
        const url = row.getValue("url") as string | undefined;
        
        if (!url) return <span className="text-slate-400">-</span>;
        
        return (
          <Button
            variant="ghost"
            size="sm"
            className="h-8 w-8 p-0"
            onClick={() => window.open(url, '_blank')}
          >
            <ExternalLink className="h-4 w-4 text-blue-600" />
          </Button>
        );
      },
      enableColumnFilter: false,
      size: 80,
    },
    {
      id: "actions",
      header: "Actions",
      cell: ({ row }) => {
        const resource = row.original;
        
        return (
          <DropdownMenu>
            <DropdownMenuTrigger asChild>
              <Button
                variant="ghost"
                className="flex h-8 w-8 p-0 data-[state=open]:bg-muted"
              >
                <MoreHorizontal className="h-4 w-4" />
                <span className="sr-only">Open menu</span>
              </Button>
            </DropdownMenuTrigger>
            <DropdownMenuContent align="end" className="w-[180px]">
              <DropdownMenuItem onClick={() => actions?.onView?.(resource)}>
                <Eye className="mr-2 h-4 w-4" />
                View Details
              </DropdownMenuItem>
              {resource.filename && (
                <DropdownMenuItem onClick={() => actions?.onDownload?.(resource)}>
                  <Download className="mr-2 h-4 w-4" />
                  Download
                </DropdownMenuItem>
              )}
              <DropdownMenuItem asChild>
                <EditLearningResourceSheet 
                  resource={resource} 
                  onSuccess={() => actions?.onSuccess?.()} 
                  trigger={
                    <div className="flex items-center w-full cursor-pointer">
                      <Edit className="mr-2 h-4 w-4" />
                      Edit
                    </div>
                  }
                />
              </DropdownMenuItem>
              <DropdownMenuSeparator />
              <DropdownMenuItem 
                onClick={() => actions?.onDelete?.(resource)}
                className="text-red-600 focus:text-red-600"
              >
                <Trash2 className="mr-2 h-4 w-4" />
                Delete
              </DropdownMenuItem>
            </DropdownMenuContent>
          </DropdownMenu>
        );
      },
      enableHiding: false,
      size: 80,
    },
  ];
} 