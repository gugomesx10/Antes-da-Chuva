import { allPages, api } from './api';
import { statusFromApi, statusToApi, urgencyFromApi, type Category, type IncidentResponse, type UpdateResponse } from '../types/backend';
import type { Incident, IncidentInput, ManagementInput, Role } from '../types/incident';
export const getCategories = () => allPages<Category>('/categories');
function mapIncident(item: IncidentResponse, categories: Category[]): Incident {
  return { ...item, id: String(item.id), category: categories.find(category => category.id === item.categoryId)?.name ?? `Categoria ${item.categoryId}`, status: statusFromApi[item.status], priority: item.urgency ? urgencyFromApi[item.urgency] : undefined };
}
export async function getIncidents(role: Role, categories: Category[]) {
  return (await allPages<IncidentResponse>(role === 'CIDADAO' ? '/incidents/mine' : '/incidents')).map(item => mapIncident(item, categories));
}
export async function getIncidentById(id: string, categories: Category[]) {
  const item = await api<IncidentResponse>(`/incidents/${encodeURIComponent(id)}`);
  const updates = await api<UpdateResponse[]>(`/incidents/${encodeURIComponent(id)}/updates`);
  return { ...mapIncident(item, categories), updates: updates.map(update => ({ ...update, id: String(update.id), status: statusFromApi[update.newStatus] })) };
}
function requestBody(input: IncidentInput) {
  if (!input.categoryId) throw new Error('Selecione uma categoria carregada do servidor.');
  return { categoryId: input.categoryId, title: input.title.trim(), description: input.description.trim(), address: input.address.trim(), neighborhood: input.neighborhood.trim(), latitude: input.latitude ?? null, longitude: input.longitude ?? null };
}
export async function saveIncident(input: IncidentInput, categories: Category[]) {
  return mapIncident(await api<IncidentResponse>('/incidents', 'POST', requestBody(input)), categories);
}
export async function updateIncident(id: string, input: IncidentInput, categories: Category[]) {
  return mapIncident(await api<IncidentResponse>(`/incidents/${encodeURIComponent(id)}`, 'PUT', requestBody(input)), categories);
}
export async function deleteIncident(id: string) {
  await api<void>(`/incidents/${encodeURIComponent(id)}`, 'DELETE');
}
export async function manageIncident(id: string, input: ManagementInput, categories: Category[]) {
  if (!input.note?.trim()) throw new Error('Informe uma observação para o morador.');
  await api(`/incidents/${encodeURIComponent(id)}/updates`, 'POST', { newStatus: statusToApi[input.status], note: input.note.trim() });
  return getIncidentById(id, categories);
}
