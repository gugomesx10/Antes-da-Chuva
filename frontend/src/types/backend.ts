import type { IncidentPriority, IncidentStatus } from './incident';
export interface Category { id: number; name: string; active: boolean }
export type BackendStatus = 'OPEN' | 'IN_REVIEW' | 'IN_PROGRESS' | 'RESOLVED' | 'REJECTED' | 'CANCELLED';
export interface IncidentResponse {
  id: number; userId: number; categoryId: number; title: string; description: string;
  address: string; neighborhood: string; latitude: number | null; longitude: number | null;
  status: BackendStatus; urgency: 'LOW' | 'MEDIUM' | 'HIGH' | 'CRITICAL' | null;
  aiSummary: string | null; aiGuidance: string | null; triageStatus: 'PENDING' | 'DONE' | 'FAILED';
  createdAt: string;
}
export interface UpdateResponse { id: number; newStatus: BackendStatus; note: string; createdAt: string }
export const statusFromApi: Record<BackendStatus, IncidentStatus> = { OPEN: 'ABERTA', IN_REVIEW: 'EM_ANALISE', IN_PROGRESS: 'EM_ANDAMENTO', RESOLVED: 'RESOLVIDA', REJECTED: 'REJEITADA', CANCELLED: 'CANCELADA' };
export const statusToApi: Record<IncidentStatus, BackendStatus> = { ABERTA: 'OPEN', EM_ANALISE: 'IN_REVIEW', EM_ANDAMENTO: 'IN_PROGRESS', RESOLVIDA: 'RESOLVED', REJEITADA: 'REJECTED', CANCELADA: 'CANCELLED' };
export const urgencyFromApi: Record<NonNullable<IncidentResponse['urgency']>, IncidentPriority> = { LOW: 'BAIXA', MEDIUM: 'MEDIA', HIGH: 'ALTA', CRITICAL: 'CRITICA' };
