import { useRef, useState } from 'react';
import { router } from 'expo-router';
import { StyleSheet, Text, View } from 'react-native';
import { useApp } from '../context/AppContext';
import { statuses, statusLabels, type Incident, type IncidentStatus } from '../types/incident';
import { IncidentHistory } from './IncidentHistory';
import { Action, Choices, colors, Field, Page, ui } from './incident-ui';

const transitions: Partial<Record<IncidentStatus, IncidentStatus[]>> = { ABERTA: ['EM_ANALISE', 'REJEITADA'], EM_ANALISE: ['EM_ANDAMENTO', 'REJEITADA'], EM_ANDAMENTO: ['RESOLVIDA', 'REJEITADA'] };

export function ManagerIncident({ item }: { item: Incident }) {
  const { updateStatus } = useApp();
  const [status, setStatus] = useState(item.status);
  const [note, setNote] = useState('');
  const [busy, setBusy] = useState(false);
  const [feedback, setFeedback] = useState<'saved' | 'error' | null>(null);
  const pending = useRef(false);
  const changed = status !== item.status && note.trim() !== '';

  async function save() {
    if (pending.current || !changed) return;
    pending.current = true;
    setBusy(true);
    setFeedback(null);
    try {
      await updateStatus(item.id, { status, note });
      setNote('');
      setFeedback('saved');
    } catch {
      setFeedback('error');
    } finally {
      pending.current = false;
      setBusy(false);
    }
  }

  return (
    <Page>
      <View style={styles.intro}>
        <View style={ui.badge}><Text style={ui.badgeText}>{statusLabels[item.status]}</Text></View>
        <Text accessibilityRole="header" style={ui.heading}>{item.title}</Text>
        <Text style={ui.subtitle}>{item.category} · {item.neighborhood}</Text>
      </View>

      <View style={[ui.card, styles.report]}>
        <Text style={ui.label}>Relato original</Text>
        <Text selectable style={ui.subtitle}>{item.description}</Text>
        <Text selectable style={ui.subtitle}>{item.address} · {item.neighborhood}</Text>
      </View>

      <View style={ui.card}>
        <Text accessibilityRole="header" style={styles.sectionTitle}>Atendimento</Text>
        <Choices
          label="Status"
          values={[item.status, ...(transitions[item.status] ?? [])].map(value => statusLabels[value])}
          value={statusLabels[status]}
          disabled={busy}
          onChange={label => {
            const next = statuses.find(value => statusLabels[value] === label);
            if (next) setStatus(next);
            setFeedback(null);
          }}
        />
        <Text style={ui.subtitle}>A prioridade é calculada pela triagem de IA no servidor.</Text>
        <Field
          label="Observação para o morador (obrigatória)"
          placeholder="Ex.: vistoria agendada"
          multiline
          maxLength={500}
          style={styles.note}
          value={note}
          editable={!busy}
          onChangeText={value => { setNote(value); setFeedback(null); }}
        />
        <Action title={busy ? 'Salvando…' : 'Salvar atendimento'} disabled={busy || !changed} onPress={() => { void save(); }} />
        {feedback === 'saved' && <Text accessibilityLiveRegion="polite" style={styles.success}>Atendimento salvo.</Text>}
        {feedback === 'error' && <Text accessibilityRole="alert" style={ui.error}>Não foi possível salvar. Tente novamente.</Text>}
      </View>

      <IncidentHistory item={item} />
      <Action title="Voltar à fila" secondary onPress={() => router.dismissTo('/gestor')} />
    </Page>
  );
}

const styles = StyleSheet.create({
  intro: { gap: 8 },
  report: { gap: 7 },
  sectionTitle: { color: colors.ink, fontSize: 20, fontWeight: '700' },
  note: { minHeight: 84 },
  success: { color: '#19634F', fontWeight: '600' },
});
