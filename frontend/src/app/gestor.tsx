import { useState } from 'react';
import { Redirect, router, useLocalSearchParams } from 'expo-router';
import { ActivityIndicator, FlatList, Pressable, StyleSheet, Text, View } from 'react-native';
import { Action, Choices, colors, Field, ui } from '../components/incident-ui';
import { useApp } from '../context/AppContext';
import { priorities, priorityLabels, statusLabels, type Incident } from '../types/incident';

const filters = ['Pendentes', 'Resolvidos', 'Todos'] as const;
const dateFormat = new Intl.DateTimeFormat('pt-BR', { day: '2-digit', month: 'short' });

function normalizeSearch(value: string) {
  return value.normalize('NFD').replace(/\p{Diacritic}/gu, '').toLocaleLowerCase('pt-BR');
}

function compareIncidents(a: Incident, b: Incident) {
  const resolved = Number(a.status === 'RESOLVIDA') - Number(b.status === 'RESOLVIDA');
  const rank = (item: Incident) => item.priority ? priorities.indexOf(item.priority) : priorities.length;
  return resolved || rank(a) - rank(b) || Date.parse(a.createdAt) - Date.parse(b.createdAt);
}

function IncidentRow({ item }: { item: Incident }) {
  return (
    <Pressable
      accessibilityRole="button"
      accessibilityLabel={`${item.title}, ${item.neighborhood}, ${statusLabels[item.status]}${item.priority ? `, prioridade ${priorityLabels[item.priority]}` : ''}`}
      accessibilityHint="Abre o atendimento do chamado"
      onPress={() => router.push({ pathname: '/detalhe/[id]', params: { id: item.id } })}
      style={({ pressed }) => [ui.card, styles.row, pressed && styles.rowPressed]}
    >
      <View style={styles.rowContent}>
        <View style={ui.row}>
          <View style={ui.badge}><Text style={ui.badgeText}>{statusLabels[item.status]}</Text></View>
          {item.priority === 'ALTA' && (
            <View style={[ui.badge, styles.urgent]}><Text style={[ui.badgeText, styles.urgentText]}>Prioridade alta</Text></View>
          )}
        </View>
        <Text style={styles.title}>{item.title}</Text>
        <Text style={ui.subtitle}>{item.category} · {item.neighborhood}</Text>
        <Text style={ui.subtitle}>
          {dateFormat.format(new Date(item.createdAt))}
          {item.priority && item.priority !== 'ALTA' ? ` · Prioridade ${priorityLabels[item.priority].toLowerCase()}` : ''}
        </Text>
      </View>
      <Text accessible={false} style={styles.arrow}>›</Text>
    </Pressable>
  );
}

export default function Manager() {
  const { incidents, loading, error, reload, logout } = useApp();
  const { id } = useLocalSearchParams<{ id?: string }>();
  const [filter, setFilter] = useState<(typeof filters)[number]>('Pendentes');
  const [search, setSearch] = useState('');
  const [logoutError, setLogoutError] = useState('');
  const term = normalizeSearch(search.trim());
  const filtered = incidents.filter(item => {
    const resolved = item.status === 'RESOLVIDA';
    if (filter === 'Resolvidos' && !resolved) return false;
    if (filter === 'Pendentes' && !['ABERTA', 'EM_ANALISE', 'EM_ANDAMENTO'].includes(item.status)) return false;
    return normalizeSearch(`${item.title} ${item.neighborhood} ${item.address} ${item.category} ${item.id}`).includes(term);
  }).sort(compareIncidents);

  if (id) return <Redirect href={{ pathname: '/detalhe/[id]', params: { id } }} />;

  return (
    <View style={ui.page}>
      <FlatList
        data={loading || error ? [] : filtered}
        contentContainerStyle={ui.content}
        keyExtractor={item => item.id}
        keyboardShouldPersistTaps="handled"
        renderItem={({ item }) => <IncidentRow item={item} />}
        ListHeaderComponent={
          <View style={styles.header}>
            <Text accessibilityRole="header" style={ui.heading}>Fila de chamados</Text>
            <Field label="Buscar chamado" placeholder="Título, bairro ou categoria" value={search} onChangeText={setSearch} />
            <Choices label="Exibir" values={filters} value={filter} onChange={setFilter} />
            {loading && <ActivityIndicator color={colors.primary} accessibilityLabel="Carregando chamados" />}
            {error ? (
              <>
                <Text accessibilityRole="alert" style={ui.error}>{error}</Text>
                <Action title="Tentar novamente" onPress={() => { void reload(); }} />
              </>
            ) : !loading && (
              <Text accessibilityLiveRegion="polite" style={ui.subtitle}>
                {filtered.length} {filtered.length === 1 ? 'chamado' : 'chamados'}
              </Text>
            )}
          </View>
        }
        ListEmptyComponent={!loading && !error ? (
          <View style={ui.card}>
            <Text style={ui.label}>
              {term ? 'Nenhum chamado encontrado.' : `Nenhum chamado${filter === 'Todos' ? '' : ` ${filter === 'Pendentes' ? 'pendente' : 'resolvido'}`}.`}
            </Text>
            {term ? <Action title="Limpar busca" secondary onPress={() => setSearch('')} /> : null}
          </View>
        ) : undefined}
        ListFooterComponent={
          <View style={styles.footer}>
            {logoutError ? <Text accessibilityRole="alert" style={ui.error}>{logoutError}</Text> : null}
            <Action title="Sair da conta" secondary onPress={() => { void logout().catch(() => setLogoutError('Não foi possível sair. Tente novamente.')); }} />
          </View>
        }
      />
    </View>
  );
}

const styles = StyleSheet.create({
  header: { gap: 16 },
  row: { flexDirection: 'row', alignItems: 'center', padding: 18 },
  rowPressed: { backgroundColor: '#F1FAF7', borderColor: colors.primary },
  rowContent: { flex: 1, gap: 5 },
  title: { color: colors.ink, fontSize: 18, fontWeight: '700', marginTop: 3 },
  arrow: { color: colors.primary, fontSize: 30, paddingHorizontal: 3 },
  urgent: { backgroundColor: '#FFF0D5' },
  urgentText: { color: '#744500' },
  footer: { gap: 12, marginTop: 8 },
});
