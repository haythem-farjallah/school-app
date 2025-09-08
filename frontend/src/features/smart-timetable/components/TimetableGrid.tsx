import React, { useState, useCallback, useMemo } from 'react';
import { DndProvider, useDrag, useDrop } from 'react-dnd';
import { HTML5Backend } from 'react-dnd-html5-backend';
import { motion, AnimatePresence } from 'framer-motion';
import { 
  Clock, 
  User, 
  MapPin, 
  BookOpen, 
  AlertTriangle, 
  CheckCircle, 
  Settings,
  Filter,
  Download,
  Maximize2,
  RotateCcw
} from 'lucide-react';
import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card';
import { Button } from '@/components/ui/button';
import { Badge } from '@/components/ui/badge';
import { Tooltip, TooltipContent, TooltipProvider, TooltipTrigger } from '@/components/ui/tooltip';
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from '@/components/ui/select';
import { Switch } from '@/components/ui/switch';
import { Label } from '@/components/ui/label';
import { Separator } from '@/components/ui/separator';
import type { 
  EnhancedTimetableSlot, 
  TimetableGridConfig, 
  DragItem, 
  DropResult,
  TimetableFilters 
} from '@/types/smart-timetable';

interface TimetableGridProps {
  timetableId: number;
  slots: EnhancedTimetableSlot[];
  config: TimetableGridConfig;
  filters: TimetableFilters;
  onSlotClick: (slot: EnhancedTimetableSlot) => void;
  onSlotDrop: (dragItem: DragItem, dropResult: DropResult) => void;
  onConfigChange: (config: Partial<TimetableGridConfig>) => void;
  onFiltersChange: (filters: Partial<TimetableFilters>) => void;
  isLoading?: boolean;
}

const DAYS_OF_WEEK = [
  'MONDAY', 'TUESDAY', 'WEDNESDAY', 'THURSDAY', 'FRIDAY', 'SATURDAY', 'SUNDAY'
];

const TIME_PERIODS = [
  { id: 1, name: 'Period 1', startTime: '08:00', endTime: '09:00', index: 1 },
  { id: 2, name: 'Period 2', startTime: '09:00', endTime: '10:00', index: 2 },
  { id: 3, name: 'Period 3', startTime: '10:00', endTime: '11:00', index: 3 },
  { id: 4, name: 'Period 4', startTime: '11:00', endTime: '12:00', index: 4 },
  { id: 5, name: 'Break', startTime: '12:00', endTime: '13:00', index: 5 },
  { id: 6, name: 'Period 5', startTime: '13:00', endTime: '14:00', index: 6 },
  { id: 7, name: 'Period 6', startTime: '14:00', endTime: '15:00', index: 7 },
  { id: 8, name: 'Period 7', startTime: '15:00', endTime: '16:00', index: 8 },
];

// Draggable Timetable Slot Component
interface DraggableSlotProps {
  slot: EnhancedTimetableSlot;
  config: TimetableGridConfig;
  onClick: (slot: EnhancedTimetableSlot) => void;
}

function DraggableSlot({ slot, config, onClick }: DraggableSlotProps) {
  const [{ isDragging }, drag] = useDrag({
    type: 'TIMETABLE_SLOT',
    item: {
      type: 'TIMETABLE_SLOT',
      slot,
      sourcePosition: {
        day: slot.dayOfWeek,
        periodId: slot.period.id
      }
    } as DragItem,
    collect: (monitor) => ({
      isDragging: monitor.isDragging(),
    }),
    canDrag: config.enableDragDrop,
  });

  const getSlotColor = useCallback(() => {
    if (slot.hasConflict && config.enableConflictHighlight) {
      return 'bg-red-100 border-red-300 text-red-900';
    }
    
    switch (config.colorScheme) {
      case 'subject':
        return slot.forCourse ? 'bg-blue-100 border-blue-300 text-blue-900' : 'bg-gray-100 border-gray-300';
      case 'teacher':
        return slot.teacher ? 'bg-green-100 border-green-300 text-green-900' : 'bg-gray-100 border-gray-300';
      case 'room':
        return slot.room ? 'bg-purple-100 border-purple-300 text-purple-900' : 'bg-gray-100 border-gray-300';
      case 'class':
        return slot.forClass ? 'bg-orange-100 border-orange-300 text-orange-900' : 'bg-gray-100 border-gray-300';
      default:
        return 'bg-white border-gray-200';
    }
  }, [slot, config]);

  const slotContent = useMemo(() => {
    if (!slot.forCourse || !slot.forClass) {
      return (
        <div className="text-center text-gray-400 text-sm">
          <div>Empty Slot</div>
        </div>
      );
    }

    return (
      <div className="space-y-1">
        {/* Course Info */}
        <div className="flex items-center justify-between">
          <div className="font-semibold text-sm truncate">
            {slot.forCourse.name}
          </div>
          {slot.hasConflict && (
            <AlertTriangle className="h-4 w-4 text-red-500 flex-shrink-0" />
          )}
          {slot.isOptimal && !slot.hasConflict && (
            <CheckCircle className="h-4 w-4 text-green-500 flex-shrink-0" />
          )}
        </div>

        {/* Class Info */}
        <div className="flex items-center text-xs text-gray-600">
          <BookOpen className="h-3 w-3 mr-1" />
          <span className="truncate">{slot.forClass.name}</span>
        </div>

        {/* Teacher Info */}
        {slot.teacher && config.showTeacherInfo && (
          <div className="flex items-center text-xs text-gray-600">
            <User className="h-3 w-3 mr-1" />
            <span className="truncate">
              {slot.teacher.firstName} {slot.teacher.lastName}
            </span>
          </div>
        )}

        {/* Room Info */}
        {slot.room && config.showRoomInfo && (
          <div className="flex items-center text-xs text-gray-600">
            <MapPin className="h-3 w-3 mr-1" />
            <span className="truncate">{slot.room.name}</span>
          </div>
        )}

        {/* Conflict Badge */}
        {slot.hasConflict && (
          <Badge variant="destructive" className="text-xs">
            {slot.conflictType || 'Conflict'}
          </Badge>
        )}
      </div>
    );
  }, [slot, config]);

  return (
    <motion.div
      ref={drag}
      className={`
        relative p-2 rounded-lg border-2 cursor-pointer transition-all duration-200
        ${getSlotColor()}
        ${isDragging ? 'opacity-50 scale-95' : 'hover:shadow-md hover:scale-105'}
        ${slot.isSelected ? 'ring-2 ring-blue-500' : ''}
        ${config.compactView ? 'p-1' : 'p-2'}
      `}
      onClick={() => onClick(slot)}
      whileHover={{ scale: 1.02 }}
      whileTap={{ scale: 0.98 }}
      layout
    >
      {slotContent}
      
      {/* Drag Handle */}
      {config.enableDragDrop && (
        <div className="absolute top-1 right-1 opacity-0 group-hover:opacity-100 transition-opacity">
          <div className="w-2 h-2 bg-gray-400 rounded-full"></div>
        </div>
      )}
    </motion.div>
  );
}

// Droppable Time Slot Component
interface DroppableSlotProps {
  day: string;
  period: typeof TIME_PERIODS[0];
  slot?: EnhancedTimetableSlot;
  config: TimetableGridConfig;
  onSlotClick: (slot: EnhancedTimetableSlot) => void;
  onDrop: (dragItem: DragItem, dropResult: DropResult) => void;
}

function DroppableSlot({ day, period, slot, config, onSlotClick, onDrop }: DroppableSlotProps) {
  const [{ isOver, canDrop }, drop] = useDrop({
    accept: 'TIMETABLE_SLOT',
    drop: (item: DragItem) => {
      const dropResult: DropResult = {
        targetDay: day,
        targetPeriodId: period.id,
      };
      onDrop(item, dropResult);
    },
    collect: (monitor) => ({
      isOver: monitor.isOver(),
      canDrop: monitor.canDrop(),
    }),
  });

  return (
    <div
      ref={drop}
      className={`
        min-h-[80px] border border-gray-200 rounded-lg transition-all duration-200
        ${isOver && canDrop ? 'bg-blue-50 border-blue-300' : ''}
        ${isOver && !canDrop ? 'bg-red-50 border-red-300' : ''}
        ${config.compactView ? 'min-h-[60px]' : 'min-h-[80px]'}
      `}
    >
      {slot ? (
        <DraggableSlot
          slot={slot}
          config={config}
          onClick={onSlotClick}
        />
      ) : (
        <div className="h-full flex items-center justify-center text-gray-400 text-sm">
          {isOver && canDrop && 'Drop here'}
          {isOver && !canDrop && 'Cannot drop'}
          {!isOver && 'Empty'}
        </div>
      )}
    </div>
  );
}

// Main Timetable Grid Component
export function TimetableGrid({
  timetableId,
  slots,
  config,
  filters,
  onSlotClick,
  onSlotDrop,
  onConfigChange,
  onFiltersChange,
  isLoading = false
}: TimetableGridProps) {
  const [selectedSlots, setSelectedSlots] = useState<number[]>([]);

  // Filter and organize slots
  const organizedSlots = useMemo(() => {
    const filtered = slots.filter(slot => {
      if (filters.teacherIds?.length && slot.teacher && !filters.teacherIds.includes(slot.teacher.id)) {
        return false;
      }
      if (filters.classIds?.length && slot.forClass && !filters.classIds.includes(slot.forClass.id)) {
        return false;
      }
      if (filters.courseIds?.length && slot.forCourse && !filters.courseIds.includes(slot.forCourse.id)) {
        return false;
      }
      if (filters.roomIds?.length && slot.room && !filters.roomIds.includes(slot.room.id)) {
        return false;
      }
      if (filters.dayOfWeek?.length && !filters.dayOfWeek.includes(slot.dayOfWeek)) {
        return false;
      }
      if (filters.conflictsOnly && !slot.hasConflict) {
        return false;
      }
      if (filters.unassignedOnly && (slot.forCourse || slot.forClass)) {
        return false;
      }
      if (filters.searchQuery) {
        const query = filters.searchQuery.toLowerCase();
        const searchableText = [
          slot.forCourse?.name,
          slot.forClass?.name,
          slot.teacher?.firstName,
          slot.teacher?.lastName,
          slot.room?.name,
          slot.description
        ].filter(Boolean).join(' ').toLowerCase();
        
        if (!searchableText.includes(query)) {
          return false;
        }
      }
      return true;
    });

    // Organize by day and period
    const organized: Record<string, Record<number, EnhancedTimetableSlot>> = {};
    
    filtered.forEach(slot => {
      if (!organized[slot.dayOfWeek]) {
        organized[slot.dayOfWeek] = {};
      }
      organized[slot.dayOfWeek][slot.period.id] = slot;
    });

    return organized;
  }, [slots, filters]);

  const displayDays = useMemo(() => {
    return config.showWeekends ? DAYS_OF_WEEK : DAYS_OF_WEEK.slice(0, 5);
  }, [config.showWeekends]);

  const handleSlotClick = useCallback((slot: EnhancedTimetableSlot) => {
    setSelectedSlots(prev => 
      prev.includes(slot.id) 
        ? prev.filter(id => id !== slot.id)
        : [...prev, slot.id]
    );
    onSlotClick(slot);
  }, [onSlotClick]);

  if (isLoading) {
    return (
      <Card className="w-full">
        <CardContent className="p-6">
          <div className="flex items-center justify-center h-96">
            <div className="animate-spin rounded-full h-12 w-12 border-b-2 border-blue-600"></div>
          </div>
        </CardContent>
      </Card>
    );
  }

  return (
    <DndProvider backend={HTML5Backend}>
      <TooltipProvider>
        <div className="space-y-6">
          {/* Header Controls */}
          <Card className="border-blue-200/60 bg-gradient-to-r from-blue-50/80 to-indigo-50/40">
            <CardHeader className="pb-4">
              <div className="flex items-center justify-between">
                <CardTitle className="text-2xl font-bold bg-gradient-to-r from-blue-900 to-indigo-700 bg-clip-text text-transparent">
                  Interactive Timetable
                </CardTitle>
                
                <div className="flex items-center gap-3">
                  {/* View Controls */}
                  <div className="flex items-center gap-2">
                    <Label htmlFor="compact-view" className="text-sm">Compact</Label>
                    <Switch
                      id="compact-view"
                      checked={config.compactView}
                      onCheckedChange={(checked) => onConfigChange({ compactView: checked })}
                    />
                  </div>
                  
                  <Separator orientation="vertical" className="h-6" />
                  
                  {/* Color Scheme */}
                  <Select
                    value={config.colorScheme}
                    onValueChange={(value: any) => onConfigChange({ colorScheme: value })}
                  >
                    <SelectTrigger className="w-32">
                      <SelectValue />
                    </SelectTrigger>
                    <SelectContent>
                      <SelectItem value="subject">By Subject</SelectItem>
                      <SelectItem value="teacher">By Teacher</SelectItem>
                      <SelectItem value="room">By Room</SelectItem>
                      <SelectItem value="class">By Class</SelectItem>
                    </SelectContent>
                  </Select>
                  
                  {/* Action Buttons */}
                  <Button variant="outline" size="sm">
                    <Filter className="h-4 w-4 mr-2" />
                    Filters
                  </Button>
                  
                  <Button variant="outline" size="sm">
                    <Download className="h-4 w-4 mr-2" />
                    Export
                  </Button>
                  
                  <Button variant="outline" size="sm">
                    <Settings className="h-4 w-4 mr-2" />
                    Settings
                  </Button>
                </div>
              </div>
            </CardHeader>
          </Card>

          {/* Timetable Grid */}
          <Card className="overflow-hidden">
            <CardContent className="p-0">
              <div className="overflow-x-auto">
                <div className="min-w-[800px]">
                  {/* Header Row */}
                  <div className="grid grid-cols-8 bg-gradient-to-r from-gray-50 to-blue-50 border-b">
                    <div className="p-4 font-semibold text-gray-700 border-r">
                      <div className="flex items-center">
                        <Clock className="h-4 w-4 mr-2" />
                        Time
                      </div>
                    </div>
                    {displayDays.map(day => (
                      <div key={day} className="p-4 font-semibold text-gray-700 border-r last:border-r-0">
                        <div className="text-center">
                          {day.charAt(0) + day.slice(1).toLowerCase()}
                        </div>
                      </div>
                    ))}
                  </div>

                  {/* Time Slots */}
                  <AnimatePresence>
                    {TIME_PERIODS.map(period => (
                      <motion.div
                        key={period.id}
                        className="grid grid-cols-8 border-b last:border-b-0"
                        initial={{ opacity: 0, y: 20 }}
                        animate={{ opacity: 1, y: 0 }}
                        exit={{ opacity: 0, y: -20 }}
                        transition={{ duration: 0.2 }}
                      >
                        {/* Time Column */}
                        <div className="p-4 bg-gray-50 border-r flex flex-col justify-center">
                          <div className="font-medium text-sm">{period.name}</div>
                          <div className="text-xs text-gray-500">
                            {period.startTime} - {period.endTime}
                          </div>
                        </div>

                        {/* Day Columns */}
                        {displayDays.map(day => (
                          <div key={`${day}-${period.id}`} className="p-2 border-r last:border-r-0">
                            <DroppableSlot
                              day={day}
                              period={period}
                              slot={organizedSlots[day]?.[period.id]}
                              config={config}
                              onSlotClick={handleSlotClick}
                              onDrop={onSlotDrop}
                            />
                          </div>
                        ))}
                      </motion.div>
                    ))}
                  </AnimatePresence>
                </div>
              </div>
            </CardContent>
          </Card>

          {/* Selection Summary */}
          {selectedSlots.length > 0 && (
            <motion.div
              initial={{ opacity: 0, y: 20 }}
              animate={{ opacity: 1, y: 0 }}
              className="fixed bottom-6 right-6 z-50"
            >
              <Card className="bg-blue-600 text-white shadow-xl">
                <CardContent className="p-4">
                  <div className="flex items-center gap-3">
                    <div className="text-sm font-medium">
                      {selectedSlots.length} slot{selectedSlots.length !== 1 ? 's' : ''} selected
                    </div>
                    <Button
                      variant="secondary"
                      size="sm"
                      onClick={() => setSelectedSlots([])}
                    >
                      Clear
                    </Button>
                  </div>
                </CardContent>
              </Card>
            </motion.div>
          )}
        </div>
      </TooltipProvider>
    </DndProvider>
  );
}
