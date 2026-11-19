import AsyncStorage from '@react-native-async-storage/async-storage';
import { api, configureSession } from './api';
import type { AuthResponse, AuthSession, RegisterInput, User } from '../types/auth';
const KEY = '@antes-da-chuva/jwt-session:v2';
async function persist(response: AuthResponse): Promise<AuthSession> {
  if (!response.accessToken || !['CIDADAO', 'GESTOR'].includes(response.user.role)) throw new Error('Resposta de autenticação inválida.');
  const session = { ...response, expiresAt: Date.now() + response.expiresIn * 1000 };
  await AsyncStorage.setItem(KEY, JSON.stringify(session));
  configureSession(session.accessToken);
  return session;
}
export const authService = {
  login: async (email: string, password: string) => persist(await api<AuthResponse>('/auth/login', 'POST', { email: email.trim().toLowerCase(), password })),
  register: async (input: RegisterInput) => persist(await api<AuthResponse>('/auth/register', 'POST', { ...input, name: input.name.trim(), email: input.email.trim().toLowerCase(), neighborhood: input.neighborhood.trim() })),
  me: () => api<User>('/auth/me'),
  async restore(): Promise<AuthSession | null> {
    try {
      const raw = await AsyncStorage.getItem(KEY);
      if (!raw) return null;
      const session: AuthSession = JSON.parse(raw);
      if (!session.accessToken || !Number.isFinite(session.expiresAt) || session.expiresAt <= Date.now()) {
        await authService.clear(); return null;
      }
      configureSession(session.accessToken);
      const user = await authService.me();
      return { ...session, user };
    } catch { await authService.clear(); return null; }
  },
  async clear() { configureSession(null); await AsyncStorage.multiRemove([KEY, '@antes-da-chuva/session:v1']); },
};
