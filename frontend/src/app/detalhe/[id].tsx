import { useState, useRef, useEffect } from 'react';
import { router, useLocalSearchParams } from 'expo-router';
import { ActivityIndicator, Text, View } from 'react-native';
import { Action, Page, ui } from '../../components/incident-ui';
import { ManagerIncident } from '../../components/ManagerIncident';
import { IncidentHistory } from '../../components/IncidentHistory';
import { statusLabels, priorityLabels } from '../../types/incident';
import { getIncidentById } from '../../services/incidentService';
import type { Incident } from '../../types/incident';
import { useApp } from '../../context/AppContext';
export default function IncidentDetails() {
  const { id } = useLocalSearchParams<{ id: string }>(); const { incidents, loading, error, reload, removeIncident, role, categories, session } = useApp();
  const [confirming, setConfirming] = useState(false);
  const [deleting, setDeleting] = useState(false);
  const [deleteError, setDeleteError] = useState('');
  const pending = useRef(false);
  async function confirmDelete() {
    if (pending.current) return;
    pending.current = true; setDeleting(true); setDeleteError('');
    try { await removeIncident(id); router.replace('/'); }
    catch (error) { setDeleteError(error instanceof Error ? error.message : 'Não foi possível cancelar a ocorrência.'); }
    finally { pending.current = false; setDeleting(false); }
  }
  const [item, setItem] = useState<Incident>();
  const [detailError, setDetailError] = useState('');
  const [detailLoading, setDetailLoading] = useState(true);
  useEffect(() => {
    let active = true;
    getIncidentById(id, categories).then(value => { if (active) { setItem(value); setDetailError(''); } })
      .catch(error => { if (active) setDetailError(error instanceof Error ? error.message : 'Erro ao consultar ocorrência.'); })
      .finally(() => { if (active) setDetailLoading(false); });
    return () => { active = false; };
  }, [id, categories, incidents]);
  if (detailLoading) return <Page><ActivityIndicator accessibilityLabel="Consultando detalhes" /></Page>;
  if (detailError) return <Page><Text style={ui.error}>{detailError}</Text><Action title="Tentar novamente" onPress={() => { void reload(); }} /></Page>;
  if (loading) return <Page><ActivityIndicator accessibilityLabel="Carregando ocorrência" /></Page>;
  if (error) return <Page><Text style={ui.error}>{error}</Text><Action title="Tentar novamente" onPress={() => { void reload(); }} /></Page>;
  if (!item) return <Page><Text style={ui.heading}>Ocorrência não encontrada</Text><Action title="Voltar aos registros" onPress={() => router.replace(role === 'GESTOR' ? '/gestor' : '/')} /></Page>;
  if (role === 'GESTOR') return <ManagerIncident item={item} key={item.id} />;
  return <Page><Text style={ui.eyebrow}>REGISTRO DA COMUNIDADE</Text><Text style={ui.heading}>{item.title}</Text><View style={ui.card}><Text style={ui.label}>Endereço</Text><Text selectable style={ui.subtitle}>{item.address}</Text><Text style={ui.label}>Tipo</Text><Text style={ui.subtitle}>{item.category}</Text><Text style={ui.label}>Bairro</Text><Text style={ui.subtitle}>{item.neighborhood}</Text><Text style={ui.label}>Status</Text><View style={ui.badge}><Text style={ui.badgeText}>●  {statusLabels[item.status]}</Text></View><Text style={ui.label}>Registrado em</Text><Text style={ui.subtitle}>{new Date(item.createdAt).toLocaleString('pt-BR')}</Text></View><Text style={ui.label}>O que foi observado</Text><Text selectable style={ui.subtitle}>{item.description}</Text><Text style={ui.label}>Prioridade de atendimento</Text><Text style={ui.subtitle}>{item.priority ? priorityLabels[item.priority] : 'Aguardando triagem'}</Text>
    <Text style={ui.label}>Triagem por IA</Text><Text style={ui.subtitle}>{item.triageStatus === 'PENDING' ? 'Aguardando processamento' : item.triageStatus === 'FAILED' ? 'Triagem pendente de nova tentativa no servidor' : 'Processada'}</Text>
    {item.aiSummary ? <Text selectable style={ui.subtitle}>{item.aiSummary}</Text> : null}
    {item.aiGuidance ? <Text selectable style={ui.subtitle}>{item.aiGuidance}</Text> : null}
    <Action title="Atualizar detalhes e triagem" secondary onPress={() => { void reload(); }} />
    <IncidentHistory item={item} />
    <Action title="Editar ocorrência" disabled={deleting || item.status !== 'ABERTA' || item.userId !== session?.user.id} onPress={() => router.push({ pathname: '/editar/[id]', params: { id: item.id } })} />
    {confirming ? <View style={ui.card}><Text style={ui.label}>Cancelar esta ocorrência?</Text><Text style={ui.subtitle}>“{item.title}” será cancelada. O histórico será preservado no servidor.</Text>{deleteError ? <Text accessibilityRole="alert" style={ui.error}>{deleteError}</Text> : null}<Action title={deleting ? 'Cancelando…' : 'Confirmar cancelamento'} disabled={deleting} onPress={() => { void confirmDelete(); }} /><Action title="Voltar" secondary disabled={deleting} onPress={() => { setConfirming(false); setDeleteError(''); }} /></View> : <Action title="Cancelar ocorrência" disabled={item.status !== 'ABERTA' || item.userId !== session?.user.id} secondary onPress={() => setConfirming(true)} />}
    <Action title="Voltar aos registros" secondary disabled={deleting} onPress={() => router.canGoBack() ? router.back() : router.replace('/')} /></Page>;
}
