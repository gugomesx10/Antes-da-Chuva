import { router, useLocalSearchParams } from 'expo-router';
import { ActivityIndicator, Text } from 'react-native';
import IncidentForm from '../../components/IncidentForm';
import { Action, Page, ui } from '../../components/incident-ui';
import { useApp } from '../../context/AppContext';

export default function EditIncident() {
  const { id } = useLocalSearchParams<{ id: string }>();
  const { incidents, loading, error, reload, editIncident } = useApp();
  if (loading) return <Page><ActivityIndicator accessibilityLabel="Carregando ocorrência" /></Page>;
  if (error) return <Page><Text style={ui.error}>{error}</Text><Action title="Tentar novamente" onPress={() => { void reload(); }} /></Page>;
  const item = incidents.find(value => value.id === id);
  if (!item || item.status !== 'ABERTA') return <Page><Text style={ui.heading}>Ocorrência não encontrada</Text><Action title="Voltar aos registros" onPress={() => router.replace('/')} /></Page>;
  return <IncidentForm key={item.id} initial={item} onSave={input => editIncident(item.id, input)} />;
}
