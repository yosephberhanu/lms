export type PropertyType =
  | 'RESIDENTIAL'
  | 'COMMERCIAL'
  | 'INDUSTRIAL'
  | 'MIXED_USE'
  | 'LAND'
  | 'OTHER';

export type OwnershipRole = 'PRIMARY_OWNER' | 'CO_OWNER' | 'BENEFICIARY' | 'TRUSTEE';

export type LeaseStatus = 'DRAFT' | 'PENDING_APPROVAL' | 'ACTIVE' | 'EXPIRED' | 'TERMINATED';

export interface Ownership {
  id?: number;
  ownerPartyId: string;
  role: OwnershipRole;
  ownershipPercentage: number;
  effectiveFrom: string;
  effectiveTo?: string | null;
  notes?: string | null;
}

export interface PropertyRecord {
  id: number;
  name: string;
  addressLine1: string;
  addressLine2?: string | null;
  city: string;
  stateOrProvince: string;
  postalCode: string;
  country: string;
  propertyType: PropertyType;
  description?: string | null;
  createdAt: string;
  updatedAt: string;
  ownerships: Ownership[];
}

export interface PropertyPayload {
  name: string;
  addressLine1: string;
  addressLine2?: string | null;
  city: string;
  stateOrProvince: string;
  postalCode: string;
  country: string;
  propertyType: PropertyType;
  description?: string | null;
  ownerships: Ownership[];
}

export interface OwnerRecord {
  id: number;
  partyId: string;
  displayName: string;
  email: string;
  phone: string;
  createdAt: string;
  updatedAt: string;
}

export interface OwnerPayload {
  partyId: string;
  displayName: string;
  email: string;
  phone: string;
}

export interface TenantRecord {
  id: number;
  externalPartyId: string | null;
  displayName: string;
  email: string | null;
  phone: string | null;
  status: string | null;
  createdAt: string;
  updatedAt: string;
}

export interface TenantPayload {
  externalPartyId?: string | null;
  displayName: string;
  email?: string | null;
  phone?: string | null;
  status?: string | null;
}

export interface LeaseStatusHistoryEntry {
  id?: number | null;
  oldStatus: LeaseStatus | null;
  newStatus: LeaseStatus;
  changedAt: string;
  changedBy?: string | null;
}

export interface LeaseRecord {
  id: number;
  propertyId: number;
  tenantId: number;
  tenant?: TenantRecord | null;
  ownerId?: string | null;
  monthlyRent: number;
  startDate: string;
  endDate: string;
  status: LeaseStatus;
  depositAmount?: number | null;
  paymentSchedule?: string | null;
  propertyNameSnapshot?: string | null;
  tenantNameSnapshot?: string | null;
  createdAt: string;
  updatedAt: string;
  /** ISO instant when owner approved (only meaningful while or after PENDING_APPROVAL). */
  ownerApprovedAt?: string | null;
  /** ISO instant when tenant approved (only meaningful while or after PENDING_APPROVAL). */
  tenantApprovedAt?: string | null;
  statusHistory?: LeaseStatusHistoryEntry[] | null;
}

export interface LeaseWritePayload {
  propertyId: number;
  tenantId: number;
  ownerId?: string | null;
  monthlyRent: number;
  startDate: string;
  endDate: string;
  depositAmount?: number | null;
  paymentSchedule?: string | null;
}
