import { createContext, useContext, useEffect, useRef, useState, type PropsWithChildren } from 'react';
import { getCategories, getIncidents, saveIncident, updateIncident, deleteIncident, manageIncident } from '../services/incidentService';
import type { Category } from '../types/backend';
import { authService } from '../services/authService';
import { configureSession } from '../services/api';
import type { AuthSession, RegisterInput } from '../types/auth';
import type { Incident, IncidentInput, ManagementInput, Role } from '../types/incident';

type AppState = {
  categories: Category[]; session: AuthSession | null; register: (input: RegisterInput) => Promise<void>;
  role: Role | null; updateStatus: (id: string, input: ManagementInput) => Promise<void>; signedIn: boolean; incidents: Incident[]; loading: boolean; error: string;
  login: (email: string, password: string) => Promise<boolean>;
  logout: () => Promise<void>; sessionLoading: boolean; reload: () => Promise<void>;
  editIncident: (id: string, input: IncidentInput) => Promise<void>;
  removeIncident: (id: string) => Promise<void>;
  addIncident: (input: IncidentInput) => Promise<string>;
};
const Context = createContext<AppState | null>(null);
export function AppProvider({ children }: PropsWithChildren) {
  const [categories, setCategories] = useState<Category[]>([]);
  const [session, setSession] = useState<AuthSession | null>(null);
  const [role, setRole] = useState<Role | null>(null);
  const [signedIn, setSignedIn] = useState(false);
  const [sessionLoading, setSessionLoading] = useState(true);
  const [incidents, setIncidents] = useState<Incident[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');
  const saving = useRef(false);
  const sessionRef = useRef<string | null>(null);
  async function reload() {
    setLoading(true); setError('');
    if (!session) { setLoading(false); return; }
    const current = session.accessToken;
    try {
      const values = await getCategories();
      const items = await getIncidents(session.user.role, values);
      if (current === sessionRef.current) { setCategories(values); setIncidents(items); }
    }
    catch (error) { if (current === sessionRef.current) setError(error instanceof Error ? error.message : 'Não foi possível carregar as ocorrências.'); }
    finally { setLoading(false); }
  }
  useEffect(() => {
    sessionRef.current = session?.accessToken ?? null;
    if (!session) return;
    let active = true;
    getCategories().then(async values => {
      const items = await getIncidents(session.user.role, values);
      if (active) { setCategories(values); setIncidents(items); setError(''); }
    }).catch(error => { if (active) setError(error instanceof Error ? error.message : 'Erro ao carregar ocorrências.'); })
      .finally(() => { if (active) setLoading(false); });
    return () => { active = false; };
  }, [session]);
  useEffect(() => {
    let active = true;
    authService.restore().then(value => { if (active) { setSession(value); setSignedIn(!!value); setRole(value?.user.role ?? null); } })
      .catch(() => { if (active) { setSession(null); setSignedIn(false); setRole(null); } })
      .finally(() => { if (active) setSessionLoading(false); });
    return () => { active = false; };
  }, []);
  function acceptSession(value: AuthSession) {
    sessionRef.current = value.accessToken; setLoading(true); setError(''); setSession(value); setRole(value.user.role); setSignedIn(true);
  }
  async function login(email: string, password: string) {
    acceptSession(await authService.login(email, password)); return true;
  }
  async function register(input: RegisterInput) { acceptSession(await authService.register(input)); }
  async function logout() {
    sessionRef.current = null; setSession(null); setSignedIn(false); setRole(null); setIncidents([]); setCategories([]); setError(''); setLoading(false);
    await authService.clear();
  }
  useEffect(() => {
    configureSession(session?.accessToken ?? null, () => { void logout().catch(() => {}); });
    if (!session) return;
    const timer = setTimeout(() => { void logout().catch(() => {}); }, Math.max(0, session.expiresAt - Date.now()));
    return () => clearTimeout(timer);
  }, [session]);
  async function addIncident(input: IncidentInput) {
    if (role !== 'CIDADAO' || loading || error || saving.current) throw new Error('Cadastro indisponível.');
    saving.current = true;
    const current = sessionRef.current;
    try {
      const item = await saveIncident(input, categories);
      if (current === sessionRef.current) setIncidents(previous => [item, ...previous]);
      return item.id;
    } finally { saving.current = false; }
  }
  async function updateStatus(id: string, input: ManagementInput) {
    if (role !== 'GESTOR' || saving.current || loading || error) throw new Error('Atualização indisponível.');
    saving.current = true;
    const current = sessionRef.current;
    try {
      const updated = await manageIncident(id, input, categories);
      if (current === sessionRef.current) setIncidents(previous => previous.map(item => item.id === id ? updated : item));
    } finally { saving.current = false; }
  }
  async function editIncident(id: string, input: IncidentInput) {
    if (role !== 'CIDADAO' || saving.current || loading || error) throw new Error('Edição indisponível.');
    saving.current = true;
    const current = sessionRef.current;
    try {
      const updated = await updateIncident(id, input, categories);
      if (current === sessionRef.current) setIncidents(previous => previous.map(item => item.id === id ? updated : item));
    } finally { saving.current = false; }
  }
  async function removeIncident(id: string) {
    if (role !== 'CIDADAO' || saving.current || loading || error) throw new Error('Exclusão indisponível.');
    saving.current = true;
    const current = sessionRef.current;
    try {
      await deleteIncident(id);
      if (current === sessionRef.current) setIncidents(previous => previous.map(item => item.id === id ? { ...item, status: 'CANCELADA' } : item));
    } finally { saving.current = false; }
  }
  return <Context.Provider value={{ categories, session, register, editIncident, removeIncident, role, updateStatus, signedIn, incidents, loading, error, login, logout, sessionLoading, reload, addIncident }}>{children}</Context.Provider>;
}
export function useApp() {
  const context = useContext(Context);
  if (!context) throw new Error('AppProvider ausente.');
  return context;
}
