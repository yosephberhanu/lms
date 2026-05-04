export interface NotificationBroadcastRequest {
  title: string;
  body: string;
  userIds: string[];
  realmRoleNames: string[];
}

export interface NotificationBroadcastResponse {
  recipientCount: number;
  messagesCreated: number;
}
