import { AxiosError } from "axios";

export const getErrorMessage = (e: AxiosError): string =>
    (e.response?.data as { message?: string } | undefined)?.message ??
    e.message ??
    "Unknown error";