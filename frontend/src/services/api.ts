let token: string | null = null;
let onUnauthorized = () => {};
const configuredUrl = process.env.EXPO_PUBLIC_API_URL?.trim();
// "same-origin": build web servido pelo próprio backend Spring; as chamadas usam caminhos relativos (/auth, /incidents...).
export const API_URL = configuredUrl === 'same-origin' ? '' : configuredUrl?.replace(/\/$/, '') || null;
export function configureSession(value: string | null, expired?: () => void) {
  token = value;
  if (expired) onUnauthorized = expired;
}
export class ApiError extends Error {
  constructor(message: string, public status: number) { super(message); }
}
export async function api<T>(path: string, method = 'GET', body?: unknown): Promise<T> {
  if (API_URL === null) throw new Error('Configure EXPO_PUBLIC_API_URL com o endereço do backend Java.');
  const controller = new AbortController();
  const timeout = setTimeout(() => controller.abort(), 20000);
  const requestToken = token;
  try {
    const response = await fetch(`${API_URL}${path}`, {
      method, signal: controller.signal,
      headers: { Accept: 'application/json', ...(body ? { 'Content-Type': 'application/json' } : {}), ...(requestToken ? { Authorization: `Bearer ${requestToken}` } : {}) },
      ...(body ? { body: JSON.stringify(body) } : {}),
    });
    if (response.status === 401 && requestToken && requestToken === token) onUnauthorized();
    if (!response.ok) {
      const detail = await response.json().catch(() => null);
      const fields = detail?.fields ? Object.values(detail.fields).join(' ') : '';
      throw new ApiError(fields || detail?.message || ({ 401: 'Sessão expirada ou credenciais inválidas.', 403: 'Você não tem permissão para esta operação.', 409: 'Esta ocorrência não pode mais ser alterada.' }[response.status] ?? `Falha na API (${response.status}).`), response.status);
    }
    if (response.status === 204) return undefined as T;
    return await response.json() as T;
  } catch (error) {
    if (error instanceof ApiError) throw error;
    throw new Error('Não foi possível conectar ao servidor. Verifique a conexão e o endereço da API.');
  } finally { clearTimeout(timeout); }
}
export interface Page<T> { content: T[]; last: boolean }
export async function allPages<T>(path: string): Promise<T[]> {
  const result: T[] = [];
  for (let page = 0; ; page++) {
    const data = await api<Page<T>>(`${path}${path.includes('?') ? '&' : '?'}page=${page}&size=100`);
    result.push(...data.content);
    if (data.last) return result;
  }
}
