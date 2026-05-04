export interface LeaseAttachment {
  id: number;
  originalFileName: string;
  contentType?: string | null;
  sizeBytes?: number | null;
  uploadedAt: string;
}

