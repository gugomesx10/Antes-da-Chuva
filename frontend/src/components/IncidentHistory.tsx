import { Text, View } from 'react-native';
import { ui } from './incident-ui';
import { priorityLabels, statusLabels, type Incident } from '../types/incident';

export function IncidentHistory({ item }: { item: Incident }) {
  return <View style={{ gap: 14 }}><Text style={ui.label}>Histórico de atendimento</Text>
    {item.updates?.length ? item.updates.map(update => <View key={update.id} style={ui.card}>
      <Text style={ui.label}>{statusLabels[update.status]} · {update.priority ? `Prioridade ${priorityLabels[update.priority].toLowerCase()}` : 'Sem prioridade'}</Text>
      {update.note ? <Text style={ui.subtitle}>{update.note}</Text> : null}
      <Text style={ui.subtitle}>{new Date(update.createdAt).toLocaleString('pt-BR')} · Gestor</Text>
    </View>) : <Text style={ui.subtitle}>Aguardando o primeiro andamento da gestão.</Text>}
    <Text style={ui.subtitle}>{new Date(item.createdAt).toLocaleString('pt-BR')} · Ocorrência registrada</Text>
  </View>;
}
