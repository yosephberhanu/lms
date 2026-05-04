import { HttpErrorResponse } from '@angular/common/http';

export function httpErrorMessage(error: unknown, fallback: string): string {
  if (error instanceof HttpErrorResponse) {
    if (typeof error.error === 'string') {
      return error.error;
    }
    const body = error.error as { message?: string } | null;
    return body?.message ?? error.message ?? fallback;
  }
  return fallback;
}
