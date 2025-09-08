import * as React from "react"
import {
  DndContext,
  DragEndEvent,
  DragOverlay,
  DragStartEvent,
  KeyboardSensor,
  PointerSensor,
  closestCenter,
  useSensor,
  useSensors,
} from "@dnd-kit/core"
import {
  SortableContext as DndSortableContext,
  arrayMove,
  sortableKeyboardCoordinates,
  verticalListSortingStrategy,
} from "@dnd-kit/sortable"
import {
  useSortable,
} from "@dnd-kit/sortable"
import { CSS } from "@dnd-kit/utilities"
import { restrictToVerticalAxis } from "@dnd-kit/modifiers"

import { cn } from "@/lib/utils"

interface SortableProps<T> {
  value: T[]
  onValueChange: (value: T[]) => void
  children: React.ReactNode
  className?: string
}

interface SortableContextProps {
  children: React.ReactNode
  className?: string
  asChild?: boolean
}

interface SortableItemProps {
  value: string
  children: React.ReactNode
  className?: string
  asChild?: boolean
}

interface SortableItemHandleProps {
  children: React.ReactNode
  className?: string
  asChild?: boolean
}

interface SortableOverlayProps {
  children: React.ReactNode
  className?: string
}

const SortableContext = React.createContext<{
  activeId: string | null
  setActiveId: (id: string | null) => void
} | null>(null)

function Sortable<T extends { id: string }>({
  value,
  onValueChange,
  children,
  className,
}: SortableProps<T>) {
  const [activeId, setActiveId] = React.useState<string | null>(null)
  const sensors = useSensors(
    useSensor(PointerSensor),
    useSensor(KeyboardSensor, {
      coordinateGetter: sortableKeyboardCoordinates,
    })
  )

  const handleDragStart = React.useCallback((event: DragStartEvent) => {
    setActiveId(event.active.id as string)
  }, [])

  const handleDragEnd = React.useCallback(
    (event: DragEndEvent) => {
      const { active, over } = event

      if (over && active.id !== over.id) {
        const oldIndex = value.findIndex((item) => item.id === active.id)
        const newIndex = value.findIndex((item) => item.id === over.id)

        onValueChange(arrayMove(value, oldIndex, newIndex))
      }

      setActiveId(null)
    },
    [value, onValueChange]
  )

  const handleDragCancel = React.useCallback(() => {
    setActiveId(null)
  }, [])

  return (
    <SortableContext.Provider value={{ activeId, setActiveId }}>
      <DndContext
        sensors={sensors}
        collisionDetection={closestCenter}
        onDragStart={handleDragStart}
        onDragEnd={handleDragEnd}
        onDragCancel={handleDragCancel}
        modifiers={[restrictToVerticalAxis]}
      >
        <DndSortableContext
          items={value.map((item) => item.id)}
          strategy={verticalListSortingStrategy}
        >
          <div className={cn("space-y-2", className)}>{children}</div>
        </DndSortableContext>
      </DndContext>
    </SortableContext.Provider>
  )
}

const SortableContent = React.forwardRef<
  HTMLDivElement,
  SortableContextProps
>(({ children, className, asChild = false, ...props }, ref) => {
  if (asChild) {
    return React.cloneElement(children as React.ReactElement, {
      ref,
      className: cn(className, (children as React.ReactElement).props.className),
      ...props,
    })
  }

  return (
    <div ref={ref} className={cn("space-y-2", className)} {...props}>
      {children}
    </div>
  )
})
SortableContent.displayName = "SortableContent"

const SortableItem = React.forwardRef<
  HTMLDivElement,
  SortableItemProps
>(({ value, children, className, asChild = false, ...props }, ref) => {
  const {
    attributes,
    listeners,
    setNodeRef,
    transform,
    transition,
    isDragging,
  } = useSortable({ id: value })

  const style = {
    transform: CSS.Transform.toString(transform),
    transition,
  }

  const combinedRef = React.useCallback(
    (node: HTMLDivElement) => {
      setNodeRef(node)
      if (typeof ref === "function") {
        ref(node)
      } else if (ref) {
        ref.current = node
      }
    },
    [setNodeRef, ref]
  )

  if (asChild) {
    return React.cloneElement(children as React.ReactElement, {
      ref: combinedRef,
      style: {
        ...style,
        ...(children as React.ReactElement).props.style,
      },
      className: cn(
        isDragging && "opacity-50",
        className,
        (children as React.ReactElement).props.className
      ),
      ...attributes,
      ...props,
    })
  }

  return (
    <div
      ref={combinedRef}
      style={style}
      className={cn(
        "relative",
        isDragging && "opacity-50",
        className
      )}
      {...attributes}
      {...props}
    >
      {children}
    </div>
  )
})
SortableItem.displayName = "SortableItem"

const SortableItemHandle = React.forwardRef<
  HTMLDivElement,
  SortableItemHandleProps
>(({ children, className, asChild = false, ...props }, ref) => {
  const { attributes, listeners } = useSortable({
    id: "handle", // This will be overridden by the parent SortableItem
  })

  if (asChild) {
    return React.cloneElement(children as React.ReactElement, {
      ref,
      className: cn(
        "cursor-grab active:cursor-grabbing",
        className,
        (children as React.ReactElement).props.className
      ),
      ...attributes,
      ...listeners,
      ...props,
    })
  }

  return (
    <div
      ref={ref}
      className={cn("cursor-grab active:cursor-grabbing", className)}
      {...attributes}
      {...listeners}
      {...props}
    >
      {children}
    </div>
  )
})
SortableItemHandle.displayName = "SortableItemHandle"

const SortableOverlay = React.forwardRef<
  HTMLDivElement,
  SortableOverlayProps
>(({ children, className, ...props }, ref) => {
  const context = React.useContext(SortableContext)

  return (
    <DragOverlay>
      {context?.activeId ? (
        <div
          ref={ref}
          className={cn(
            "rounded-md border bg-background shadow-lg",
            className
          )}
          {...props}
        >
          {children}
        </div>
      ) : null}
    </DragOverlay>
  )
})
SortableOverlay.displayName = "SortableOverlay"

export {
  Sortable,
  SortableContent,
  SortableItem,
  SortableItemHandle,
  SortableOverlay,
}
