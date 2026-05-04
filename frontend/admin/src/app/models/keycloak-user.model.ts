export interface KeycloakUser {
  id: string;
  username: string;
  email: string | null;
  phone: string | null;
  firstName: string | null;
  lastName: string | null;
  realmRoles: string[];
  enabled: boolean;
}

export interface KeycloakUserCreate {
  username: string;
  email?: string | null;
  phone?: string | null;
  firstName?: string | null;
  lastName?: string | null;
  password?: string | null;
  realmRoles?: string[] | null;
  enabled: boolean;
}

export interface KeycloakUserUpdate {
  email?: string | null;
  phone?: string | null;
  firstName?: string | null;
  lastName?: string | null;
  password?: string | null;
  realmRoles?: string[] | null;
  enabled?: boolean | null;
}
