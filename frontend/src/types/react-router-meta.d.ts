// src/routes/types.ts  (or anywhere you like)
export type TitleValue =
  | string
  | ((ctx: { params: Record<string, string>; pathname: string }) => string);

export interface TitleHandle {
  title?: TitleValue;
}
