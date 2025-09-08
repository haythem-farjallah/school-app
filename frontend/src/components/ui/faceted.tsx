import * as React from "react"
import * as PopoverPrimitive from "@radix-ui/react-popover"
import { Check, ChevronsUpDown, X } from "lucide-react"

import { cn } from "@/lib/utils"
import { Badge } from "@/components/ui/badge"
import { Command, CommandEmpty, CommandGroup, CommandInput, CommandItem, CommandList } from "@/components/ui/command"

const Faceted = (props: React.ComponentPropsWithoutRef<typeof PopoverPrimitive.Root>) => (
  <PopoverPrimitive.Root {...props} />
)
Faceted.displayName = "Faceted"

const FacetedTrigger = React.forwardRef<
  React.ElementRef<typeof PopoverPrimitive.Trigger>,
  React.ComponentPropsWithoutRef<typeof PopoverPrimitive.Trigger>
>(({ className, children, ...props }, ref) => (
  <PopoverPrimitive.Trigger
    ref={ref}
    className={cn(
      "flex h-10 w-full items-center justify-between rounded-md border border-gray-300 bg-white px-3 py-2 text-sm text-gray-900 placeholder:text-gray-500 focus:outline-none focus:ring-2 focus:ring-blue-500 focus:border-blue-500 disabled:cursor-not-allowed disabled:opacity-50 disabled:bg-gray-50 [&>span]:line-clamp-1",
      className
    )}
    {...props}
  >
    {children}
    <ChevronsUpDown className="h-4 w-4 opacity-50" />
  </PopoverPrimitive.Trigger>
))
FacetedTrigger.displayName = "FacetedTrigger"

const FacetedContent = React.forwardRef<
  React.ElementRef<typeof PopoverPrimitive.Content>,
  React.ComponentPropsWithoutRef<typeof PopoverPrimitive.Content>
>(({ className, children, ...props }, ref) => (
  <PopoverPrimitive.Content
    ref={ref}
    className={cn(
      "z-50 min-w-[8rem] overflow-hidden rounded-md border border-gray-200 bg-white p-0 text-gray-900 shadow-lg data-[state=open]:animate-in data-[state=closed]:animate-out data-[state=closed]:fade-out-0 data-[state=open]:fade-in-0 data-[state=closed]:zoom-out-95 data-[state=open]:zoom-in-95 data-[side=bottom]:slide-in-from-top-2 data-[side=left]:slide-in-from-right-2 data-[side=right]:slide-in-from-left-2 data-[side=top]:slide-in-from-bottom-2",
      className
    )}
    {...props}
  >
    <Command>{children}</Command>
  </PopoverPrimitive.Content>
))
FacetedContent.displayName = "FacetedContent"

const FacetedInput = React.forwardRef<
  React.ElementRef<typeof CommandInput>,
  React.ComponentPropsWithoutRef<typeof CommandInput>
>(({ className, ...props }, ref) => (
  <CommandInput
    ref={ref}
    className={cn("h-9", className)}
    {...props}
  />
))
FacetedInput.displayName = "FacetedInput"

const FacetedList = React.forwardRef<
  React.ElementRef<typeof CommandList>,
  React.ComponentPropsWithoutRef<typeof CommandList>
>(({ className, ...props }, ref) => (
  <CommandList
    ref={ref}
    className={cn("max-h-[200px] overflow-y-auto", className)}
    {...props}
  />
))
FacetedList.displayName = "FacetedList"

const FacetedEmpty = React.forwardRef<
  React.ElementRef<typeof CommandEmpty>,
  React.ComponentPropsWithoutRef<typeof CommandEmpty>
>(({ ...props }, ref) => (
  <CommandEmpty ref={ref} {...props} />
))
FacetedEmpty.displayName = "FacetedEmpty"

const FacetedGroup = React.forwardRef<
  React.ElementRef<typeof CommandGroup>,
  React.ComponentPropsWithoutRef<typeof CommandGroup>
>(({ ...props }, ref) => (
  <CommandGroup ref={ref} {...props} />
))
FacetedGroup.displayName = "FacetedGroup"

const FacetedItem = React.forwardRef<
  React.ElementRef<typeof CommandItem>,
  React.ComponentPropsWithoutRef<typeof CommandItem> & {
    checked?: boolean
  }
>(({ className, children, checked, ...props }, ref) => (
  <CommandItem
    ref={ref}
    className={cn(
      "relative flex cursor-default select-none items-center rounded-sm px-2 py-1.5 text-sm text-gray-900 outline-none hover:bg-gray-100 aria-selected:bg-gray-100 data-[disabled]:pointer-events-none data-[disabled]:opacity-50",
      className
    )}
    {...props}
  >
    <div className="flex items-center space-x-2">
      <div className="flex h-4 w-4 items-center justify-center">
        {checked && <Check className="h-4 w-4 text-blue-600" />}
      </div>
      <span>{children}</span>
    </div>
  </CommandItem>
))
FacetedItem.displayName = "FacetedItem"

const FacetedBadgeList = React.forwardRef<
  HTMLDivElement,
  React.HTMLAttributes<HTMLDivElement> & {
    options?: Array<{ label: string; value: string; icon?: React.ComponentType; count?: number }>
    placeholder?: string
    selectedValues?: string[]
    onRemove?: (value: string) => void
    maxDisplay?: number
  }
>(({ className, options = [], placeholder = "Select options...", selectedValues = [], onRemove, maxDisplay = 3, ...props }, ref) => {
  const selectedOptions = options.filter(option => selectedValues.includes(option.value))
  
  if (selectedOptions.length === 0) {
    return (
      <div ref={ref} className={cn("flex items-center", className)} {...props}>
        <span className="text-muted-foreground">{placeholder}</span>
      </div>
    )
  }

  const displayedOptions = selectedOptions.slice(0, maxDisplay)
  const remainingCount = selectedOptions.length - maxDisplay

  return (
    <div ref={ref} className={cn("flex flex-wrap items-center gap-1", className)} {...props}>
      {displayedOptions.map((option) => (
        <Badge
          key={option.value}
          variant="secondary"
          className="flex items-center gap-1 px-2 py-1 text-xs"
        >
          {option.icon && <option.icon />}
          <span>{option.label}</span>
          {onRemove && (
            <button
              type="button"
              onClick={(e) => {
                e.preventDefault()
                e.stopPropagation()
                onRemove(option.value)
              }}
              className="ml-1 h-3 w-3 rounded-full hover:bg-muted-foreground/20"
            >
              <X className="h-2 w-2" />
            </button>
          )}
        </Badge>
      ))}
      {remainingCount > 0 && (
        <Badge variant="secondary" className="px-2 py-1 text-xs">
          +{remainingCount} more
        </Badge>
      )}
    </div>
  )
})
FacetedBadgeList.displayName = "FacetedBadgeList"

export {
  Faceted,
  FacetedTrigger,
  FacetedContent,
  FacetedInput,
  FacetedList,
  FacetedEmpty,
  FacetedGroup,
  FacetedItem,
  FacetedBadgeList,
}
