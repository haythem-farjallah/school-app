import * as React from "react";
import { getCoreRowModel, useReactTable, flexRender, type PaginationState, type Updater } from "@tanstack/react-table";
import { Plus, Search, SearchX, Users, X } from "lucide-react";
import { parseAsInteger, parseAsString, useQueryState } from "nuqs";
import { useTranslation } from "react-i18next";
import { Link } from "react-router-dom";

import { PageContainer } from "@/components/Layout/PageContainer";
import { PageHeader } from "@/components/Layout/PageHeader";
import { EmptyState } from "@/components/Shared/EmptyState";
import { ErrorState } from "@/components/Shared/ErrorState";
import { DataTablePagination } from "@/components/data-table/data-table-pagination";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Skeleton } from "@/components/ui/skeleton";
import type { Student } from "@/types/student";
import { useStudents } from "../hooks/use-students";
import { useStudentsPath } from "../paths";
import { getStudentColumns } from "./student-columns";
import { StudentMobileList } from "./student-mobile-list";
import { EditStudentSheet } from "./student-sheet";
import { DeleteStudentDialog } from "./delete-student-dialog";

const SEARCH_DEBOUNCE_MS = 300;

/** The students list shared by the admin and staff areas. */
export function StudentsPage() {
  const { t } = useTranslation();
  const basePath = useStudentsPath();

  // Search and pagination live in the URL, so they survive reloads and can be shared.
  const [query, setQuery] = useQueryState("q", parseAsString.withDefault(""));
  const [page, setPage] = useQueryState("page", parseAsInteger.withDefault(1));
  const [perPage, setPerPage] = useQueryState("perPage", parseAsInteger.withDefault(10));
  const [searchInput, setSearchInput] = React.useState(query);
  const searchTimer = React.useRef<number>();
  React.useEffect(() => () => window.clearTimeout(searchTimer.current), []);

  const changeSearch = (value: string) => {
    setSearchInput(value);
    window.clearTimeout(searchTimer.current);
    searchTimer.current = window.setTimeout(() => {
      void setQuery(value.trim() || null);
      void setPage(null);
    }, SEARCH_DEBOUNCE_MS);
  };

  const clearSearch = () => {
    window.clearTimeout(searchTimer.current);
    setSearchInput("");
    void setQuery(null);
    void setPage(null);
  };

  const search = query.trim();
  const { data, isPending, isError, isFetching, isPlaceholderData, refetch } = useStudents({
    page: page - 1,
    size: perPage,
    search,
  });
  const students = data?.data ?? [];
  const totalPages = data?.totalPages ?? 0;

  // Deleting the last student of the last page leaves the page empty; step back to the new last page.
  React.useEffect(() => {
    if (data && !isFetching && students.length === 0 && totalPages > 0 && page > totalPages) {
      void setPage(totalPages);
    }
  }, [data, isFetching, students.length, totalPages, page, setPage]);

  const [editing, setEditing] = React.useState<Student | null>(null);
  const [editOpen, setEditOpen] = React.useState(false);
  const [deleting, setDeleting] = React.useState<Student | null>(null);
  const [deleteOpen, setDeleteOpen] = React.useState(false);

  const openEdit = React.useCallback((student: Student) => {
    setEditing(student);
    setEditOpen(true);
  }, []);
  const openDelete = React.useCallback((student: Student) => {
    setDeleting(student);
    setDeleteOpen(true);
  }, []);

  const columns = React.useMemo(
    // `t` changes with the language, which re-creates the translated headers.
    () => getStudentColumns({ t, basePath, onEdit: openEdit, onDelete: openDelete }),
    [t, basePath, openEdit, openDelete],
  );

  const pagination: PaginationState = { pageIndex: page - 1, pageSize: perPage };
  const onPaginationChange = (updater: Updater<PaginationState>) => {
    const next = typeof updater === "function" ? updater(pagination) : updater;
    void setPage(next.pageIndex + 1);
    void setPerPage(next.pageSize);
  };

  const table = useReactTable({
    data: students,
    columns,
    pageCount: totalPages,
    state: { pagination },
    onPaginationChange,
    manualPagination: true,
    enableRowSelection: false,
    enableSorting: false,
    getCoreRowModel: getCoreRowModel(),
  });

  const addStudent = (
    <Button asChild>
      <Link to={`${basePath}/create`}>
        <Plus aria-hidden="true" />
        {t("students.list.add")}
      </Link>
    </Button>
  );

  let content: React.ReactNode;
  if (isPending) {
    content = <StudentsListSkeleton rows={Math.min(perPage, 8)} label={t("students.list.loading")} />;
  } else if (isError) {
    content = (
      <ErrorState
        title={t("students.error.listTitle")}
        description={t("students.error.listDescription")}
        onRetry={() => void refetch()}
      />
    );
  } else if (students.length === 0 && search) {
    content = (
      <EmptyState
        icon={SearchX}
        title={t("students.noResults.title")}
        description={t("students.noResults.description", { query: search })}
        action={
          <Button variant="outline" onClick={clearSearch}>
            {t("students.search.clear")}
          </Button>
        }
      />
    );
  } else if (students.length === 0 && page === 1) {
    content = (
      <EmptyState
        icon={Users}
        title={t("students.empty.title")}
        description={t("students.empty.description")}
        action={addStudent}
      />
    );
  } else {
    content = (
      <div aria-busy={isFetching} className={isPlaceholderData ? "opacity-60 transition-opacity" : undefined}>
        <div className="hidden overflow-x-auto md:block">
          <table aria-label={t("students.table.label")} className="w-full text-sm">
            <thead className="bg-muted/50">
              {table.getHeaderGroups().map((headerGroup) => (
                <tr key={headerGroup.id}>
                  {headerGroup.headers.map((header) => (
                    <th
                      key={header.id}
                      scope="col"
                      className={
                        header.column.id === "actions"
                          ? "w-14 px-4"
                          : "h-11 px-4 text-start align-middle text-xs font-medium uppercase tracking-wide text-muted-foreground"
                      }
                    >
                      {flexRender(header.column.columnDef.header, header.getContext())}
                    </th>
                  ))}
                </tr>
              ))}
            </thead>
            <tbody className="divide-y divide-border">
              {table.getRowModel().rows.map((row) => (
                <tr key={row.id} className="transition-colors hover:bg-muted/40">
                  {row.getVisibleCells().map((cell) => (
                    <td key={cell.id} className="px-4 py-3 align-middle text-foreground">
                      {flexRender(cell.column.columnDef.cell, cell.getContext())}
                    </td>
                  ))}
                </tr>
              ))}
            </tbody>
          </table>
        </div>
        <div className="md:hidden">
          <StudentMobileList students={students} basePath={basePath} onEdit={openEdit} onDelete={openDelete} />
        </div>
        <div className="border-t border-border px-4 py-3">
          <DataTablePagination table={table} />
        </div>
      </div>
    );
  }

  return (
    <PageContainer>
      <PageHeader title={t("students.list.title")} description={t("students.list.description")} actions={addStudent} />

      <section
        aria-label={t("students.list.title")}
        className="overflow-hidden rounded-xl border border-border bg-card text-card-foreground shadow-xs"
      >
        <div className="flex flex-col gap-3 border-b border-border p-4 sm:flex-row sm:items-center sm:justify-between">
          <div className="relative w-full sm:max-w-sm">
            <Search
              aria-hidden="true"
              className="pointer-events-none absolute start-3 top-1/2 size-4 -translate-y-1/2 text-muted-foreground"
            />
            <Input
              type="search"
              value={searchInput}
              onChange={(event) => changeSearch(event.target.value)}
              placeholder={t("students.search.placeholder")}
              aria-label={t("students.search.label")}
              className="ps-9 pe-9 [&::-webkit-search-cancel-button]:hidden"
            />
            {searchInput && (
              <Button
                type="button"
                variant="ghost"
                size="icon"
                onClick={clearSearch}
                aria-label={t("students.search.clear")}
                className="absolute end-1 top-1/2 size-8 -translate-y-1/2 text-muted-foreground"
              >
                <X aria-hidden="true" />
              </Button>
            )}
          </div>
          {data && !isError && (
            <p className="text-sm text-muted-foreground" aria-live="polite">
              {search
                ? t("students.list.results", { count: data.totalItems })
                : t("students.list.total", { count: data.totalItems })}
            </p>
          )}
        </div>
        {content}
      </section>

      <EditStudentSheet student={editing} open={editOpen} onOpenChange={setEditOpen} />
      <DeleteStudentDialog student={deleting} open={deleteOpen} onOpenChange={setDeleteOpen} />
    </PageContainer>
  );
}

function StudentsListSkeleton({ rows, label }: { rows: number; label: string }) {
  return (
    <div role="status" aria-label={label}>
      <div className="hidden md:block">
        <div className="flex gap-4 bg-muted/50 px-4 py-4">
          <Skeleton className="h-3 w-20" />
          <Skeleton className="h-3 w-28" />
          <Skeleton className="h-3 w-14" />
          <Skeleton className="h-3 w-12" />
          <Skeleton className="h-3 w-10" />
        </div>
        {Array.from({ length: rows }, (_, index) => (
          <div key={index} className="flex items-center gap-4 border-t border-border px-4 py-4">
            <Skeleton className="h-4 w-40" />
            <Skeleton className="h-4 w-56" />
            <Skeleton className="h-4 w-24" />
            <Skeleton className="h-4 w-16" />
            <Skeleton className="h-4 w-16" />
            <Skeleton className="ms-auto size-8" />
          </div>
        ))}
      </div>
      <div className="md:hidden">
        {Array.from({ length: Math.min(rows, 5) }, (_, index) => (
          <div key={index} className="space-y-2 border-t border-border px-4 py-4 first:border-t-0">
            <Skeleton className="h-4 w-40" />
            <Skeleton className="h-3 w-56" />
            <Skeleton className="h-3 w-32" />
          </div>
        ))}
      </div>
    </div>
  );
}
