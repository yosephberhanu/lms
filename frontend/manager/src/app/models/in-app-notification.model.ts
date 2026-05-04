export interface InAppNotification {
  id: string;
  title: string;
  body: string;
  createdAt: string;
  readAt: string | null;
}

export interface SpringPage<T> {
  content: T[];
  totalElements?: number;
}
