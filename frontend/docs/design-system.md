# Design system

The rules new frontend UI follows. Existing screens migrate to them in their own packets.

## Product character

Modern educational SaaS.
Friendly and visual for learning experiences.
Calm and efficient for administration.

## Source of truth

- `src/styles/design-tokens.css` defines every global visual token (color, radius, shadow).
- `src/styles/role-themes.css` sets the per-role `--role-accent` / `--role-accent-soft`.
- `tailwind.config.js` maps tokens to semantic classes. `src/index.css` only imports tokens and holds global base rules.
- The stack is Tailwind, the local shadcn/Radix primitives in `src/components/ui`, and Lucide icons. Do not add another UI kit or icon library.

New code uses semantic classes:

| Purpose | Classes |
| --- | --- |
| Page canvas | `bg-background text-foreground` |
| Surfaces | `bg-card text-card-foreground`, `bg-popover` |
| Supporting text | `text-muted-foreground` |
| Lines | `border-border`, controls `border-input` |
| Brand action | `bg-primary text-primary-foreground hover:bg-primary-hover` |
| Brand tint | `bg-primary-soft`, `bg-accent text-accent-foreground` |
| Quiet fills | `bg-secondary`, `bg-muted` |
| Status | `destructive`, `success`, `warning`, `info` (each with `-foreground`; the last three also `-soft`) |
| Focus | `ring-ring` |
| Role identity | `bg-role-accent`, `bg-role-accent-soft`, `border-role-accent` |
| Learning content | `learning-green`, `learning-yellow`, `learning-coral`, `learning-blue` |

Do not use raw palette classes (`bg-blue-500`, `text-red-600`, `from-purple-50`) in new code unless the color carries real domain meaning that no token covers. Legacy hard-coded colors stay until their migration packet.

`src/lib/theme.ts` (`getRoleClasses`, per-role hex themes) is a legacy compatibility layer. Do not use it in new code.

## Brand principles

- Teal is the application brand. There is one brand, not one per role.
- Pale ice-blue page background, white surfaces, dark readable text.
- Bright colors have meaning: status, learning content, or identity. They are not decoration.
- Role colors are secondary accents only (ADMIN coral, TEACHER green, STUDENT blue, STAFF violet, PARENT amber). They never replace the brand on buttons, links or focus rings.
- Learning views may be more expressive. Admin and staff workflows stay restrained.
- The app is light-first. Dark mode is not supported yet; do not add `dark:` styling to new code.

## Spacing, radius, elevation, type

Use Tailwind's spacing scale. Preferred rhythm: 4, 8, 12, 16, 24, 32, 48 px (`1`, `2`, `3`, `4`, `6`, `8`, `12`). Avoid one-off arbitrary values.

Radius (`--radius` is 8px):

| Element | Class |
| --- | --- |
| Small controls, badges | `rounded-md` (6px) |
| Buttons, inputs, selects | `rounded-lg` (8px) |
| Cards | `rounded-xl` (12px) |
| Dialogs, sheets, popovers | `rounded-xl` or `rounded-2xl` (12–16px) |

Do not reach for `rounded-2xl`/`rounded-3xl` as decoration.

Elevation: structure comes from borders and whitespace. Shadows are secondary: `shadow-xs`/`shadow-sm` for raised controls or cards that need separation, `shadow-md` for hover lift, `shadow-overlay` for popovers, menus, dialogs.

Control height: 40px default (`h-10`); 44px minimum (`h-11`) for touch-heavy or important mobile controls.

Typography:

| Role | Size / weight |
| --- | --- |
| Page title | 28–32px, semibold (`text-3xl font-semibold`) |
| Section title | 18–20px, semibold (`text-lg`/`text-xl font-semibold`) |
| Card title | 15–16px, medium/semibold (`text-base font-semibold`) |
| Body | 14–16px, regular (`text-sm`/`text-base`) |
| Supporting text | 12–14px, muted (`text-xs`/`text-sm text-muted-foreground`) |

## Layout

```text
App shell
  → Page container
    → Page header
      → title
      → optional description / breadcrumb
      → actions
    → optional summary / stat area
    → page content
```

Do not automatically wrap the entire Outlet/page inside one giant white card. The page sits on the ice-blue canvas; cards represent meaningful sections or content, not the whole application canvas.

## Page states

Every async feature handles: Loading, Loaded, Empty, Error, Unavailable. Unavailable features say so plainly; they never simulate success.

## Forms

- Labels above controls; helper and error text below.
- Clear required indicators.
- Inline validation (React Hook Form + Zod).
- One obvious primary action; destructive actions visually distinct.
- Large forms use a page. Medium focused tasks may use a sheet. Small focused tasks may use a dialog. Never put a large form in a small modal.

## Dialogs

For focused actions: send a message, assign something, confirm a destructive action, change a status.

## Tables

- Consistent row density and consistent row actions.
- Loading, empty and error states inside the table area.
- Search and filters above the table, in a consistent place.
- Consistent pagination and sorting patterns (TanStack Table).
- Horizontal behavior on small screens is handled explicitly (scroll container or a card list), never by page overflow.

## Accessibility

- Everything keyboard usable, with visible focus (`ring-ring`).
- Semantic controls (`button`, `a`, `label`, native inputs or Radix primitives).
- Icon-only actions have an accessible name, plus a tooltip where the meaning is not obvious.
- Adequate touch targets (see control height).
- Never communicate meaning by color alone; pair color with text or an icon.
- Text meets WCAG AA contrast. `warning` fills take dark text; role and learning accents are not for small text on white.

## Learning UX vs management UX

Learning pages (student resources, courses, progress) may use stronger imagery, progress indicators, course cards, friendly illustrations or icons, and the brighter learning accents.

Management pages (admin, staff, teacher back office) prioritize clarity, scanability, forms, tables and efficient workflows.

## Originality

References such as Bricks/Quest/National Geographic Learning are mood and UX references only. Do not reproduce their proprietary assets, logos, exact layouts, wording, or brand identity.
