/* eslint-disable @typescript-eslint/no-explicit-any */
import { useState } from "react";
import { useTranslation } from "react-i18next";
import { useMutation, useQueryClient } from "@tanstack/react-query";
import { createColumnHelper, ColumnDef, SortingState, RowSelectionState } from "@tanstack/react-table";
import * as z from "zod";

import { Level } from "@/types/level";
import { usePaginated } from "@/hooks/usePaginated";
import { DataTable, ServerTableState } from "@/components/Table/DataTable";
import { Button } from "@/components/ui/button";
import { Dialog, DialogContent, DialogHeader, DialogTitle, DialogTrigger } from "@/components/ui/dialog";
import { AutoForm } from "@/form/AutoForm";
import { Plus, Eye, Edit, Trash2 } from "lucide-react";
import { http } from "@/lib/http";
import type { FormRecipe } from "@/form/types";
import { Badge } from "@/components/ui/badge";
import { Checkbox } from "@/components/ui/checkbox";

const columnHelper = createColumnHelper<Level>();

const columns = [
  {
    id: "select",
    header: ({ table }: any) => (
      <Checkbox
        checked={table.getIsAllPageRowsSelected()}
        onCheckedChange={(value) => table.toggleAllPageRowsSelected(!!value)}
        aria-label="Select all"
      />
    ),
    cell: ({ row }: any) => (
      <Checkbox
        checked={row.getIsSelected()}
        onCheckedChange={(value) => row.toggleSelected(!!value)}
        aria-label="Select row"
      />
    ),
    enableSorting: false,
    enableHiding: false,
  },
  columnHelper.accessor("id", {
    header: "ID",
    cell: (info) => info.getValue(),
  }),
  columnHelper.accessor("name", {
    header: "Name",
    cell: (info) => info.getValue(),
  }),
  columnHelper.accessor((row) => row.courseIds.length, {
    id: "courses",
    header: "Courses Count",
    cell: (info) => <Badge variant="secondary">{info.getValue()}</Badge>,
    enableSorting: false,
  }),
  {
    id: "actions",
    header: "Actions",
    cell: ({ row }: any) => {
      const levelData = row.original;
      return (
        <div className="flex items-center space-x-2">
          <Button
            variant="ghost"
            size="sm"
            onClick={() => console.log("View", levelData.id)}
          >
            <Eye className="h-4 w-4" />
          </Button>
          <Button
            variant="ghost"
            size="sm"
            onClick={() => console.log("Edit", levelData.id)}
          >
            <Edit className="h-4 w-4" />
          </Button>
          <Button
            variant="ghost"
            size="sm"
            onClick={() => console.log("Delete", levelData.id)}
          >
            <Trash2 className="h-4 w-4 text-red-600" />
          </Button>
        </div>
      );
    },
    enableSorting: false,
  },
] as ColumnDef<Level, any>[];

const createLevelSchema = z.object({
  name: z.string().min(2, "Name is required"),
});

type CreateLevelInput = z.infer<typeof createLevelSchema>;

const Levels = () => {
  const { t } = useTranslation();
  const qc = useQueryClient();

  /* ─── Table UI state ─────────── */
  const [sorting, setSorting] = useState<SortingState>([]);
  const [rowSelection, setRowSelection] = useState<RowSelectionState>({});
  const [search, setSearch] = useState<string>("");

  /* ─── Data fetching (paginated hook) ──────────────────────────────── */
  const limit = 10;
  const sortParam = sorting.length > 0 ? `${sorting[0].id},${sorting[0].desc ? "desc" : "asc"}` : undefined;

  // Build params with search if provided
  const params: Record<string, unknown> = {};
  if (sortParam) params.sort = sortParam;
  if (search) params.nameLike = search;

  const {
    data: pageData,
    isLoading,
    page,
    setPage,
  } = usePaginated<Level>(
    "/v1/levels",
    "levels",
    limit,
    params,
  );

  const tableState: ServerTableState = {
    pagination: { pageIndex: page, pageSize: limit },
    sorting,
    search,
    rowSelection,
  };

  const handleStateChange = (s: ServerTableState) => {
    if (s.pagination.pageIndex !== page) setPage(s.pagination.pageIndex);
    if (s.sorting !== sorting) setSorting(s.sorting);
    if (s.search !== search) setSearch(s.search || "");
    if (s.rowSelection !== rowSelection) setRowSelection(s.rowSelection || {});
  };

  /* ─── Mutation: create level ─────────────────────── */
  const createMutation = useMutation({
    mutationFn: (payload: CreateLevelInput) => http.post("/api/v1/levels", payload),
    onSuccess: () => qc.invalidateQueries({ queryKey: ["levels"] }),
  });

  /* ─── Form recipe for AutoForm ───────────────────── */
  const createLevelRecipe = {
    schema: createLevelSchema,
    fields: [
      { name: "name", type: "text" as const, label: "Name", placeholder: "Level name" },
    ] as any,
    onSubmit: async (values: unknown) => {
      await createMutation.mutateAsync(values as CreateLevelInput);
    },
  } as FormRecipe;

  return (
    <div className="p-6 space-y-6">
      <div className="flex items-center justify-between">
        <h1 className="text-2xl font-bold">{t("Levels Management")}</h1>

        <Dialog>
          <DialogTrigger asChild>
            <Button>
              <Plus className="h-4 w-4 mr-2" /> {t("Add Level")}
            </Button>
          </DialogTrigger>
          <DialogContent className="sm:max-w-md">
            <DialogHeader>
              <DialogTitle>{t("Create Level")}</DialogTitle>
            </DialogHeader>
            <AutoForm recipe={createLevelRecipe} submitLabel="Create" />
          </DialogContent>
        </Dialog>
      </div>

      <div className="bg-white rounded-lg shadow p-4">
        <DataTable<Level>
          data={pageData?.data ?? []}
          total={pageData?.totalItems ?? 0}
          columns={columns}
          state={tableState}
          onStateChange={handleStateChange}
          loading={isLoading}
          searchPlaceholder="Search levels by name..."
          enableRowSelection={true}
        />
      </div>
    </div>
  );
};

export default Levels; 