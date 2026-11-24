export const priorities = ['CRITICA', 'ALTA', 'MEDIA', 'BAIXA'] as const;
export type IncidentPriority = typeof priorities[number];
export const priorityLabels: Record<IncidentPriority, string> = { CRITICA: 'Crítica', ALTA: 'Alta', MEDIA: 'Média', BAIXA: 'Baixa' };
export interface IncidentUpdate { id: string; status: IncidentStatus; priority?: IncidentPriority; note: string; createdAt: string }
export interface ManagementInput { status: IncidentStatus; priority?: IncidentPriority; note?: string }
export const statuses = ['ABERTA', 'EM_ANALISE', 'EM_ANDAMENTO', 'RESOLVIDA', 'REJEITADA', 'CANCELADA'] as const;
export type IncidentStatus = typeof statuses[number];
export const statusLabels: Record<IncidentStatus, string> = { REJEITADA: 'Rejeitada', CANCELADA: 'Cancelada', ABERTA: 'Aberta', EM_ANALISE: 'Em análise', EM_ANDAMENTO: 'Em andamento', RESOLVIDA: 'Resolvida' };
export interface Incident {
  id: string;
  title: string;
  description: string;
  category: string;
  categoryId?: number; userId?: number; latitude?: number | null; longitude?: number | null;
  aiSummary?: string | null; aiGuidance?: string | null; triageStatus?: 'PENDING' | 'DONE' | 'FAILED';
  address: string;
  neighborhood: string;
  status: IncidentStatus;
  priority?: IncidentPriority;
  updates?: IncidentUpdate[];
  createdAt: string;
}
export type IncidentInput = Pick<Incident, 'title' | 'description' | 'category' | 'address' | 'neighborhood' | 'categoryId' | 'latitude' | 'longitude'>;
export type Role = 'CIDADAO' | 'GESTOR';
