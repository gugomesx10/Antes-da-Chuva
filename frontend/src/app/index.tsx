import { useState } from 'react';
import { Redirect, router } from 'expo-router';
import { ActivityIndicator, FlatList, Pressable, Text, View } from 'react-native';
import { Action, colors, Field, ui } from '../components/incident-ui';
import { statusLabels } from '../types/incident';
import { useApp } from '../context/AppContext';

export default function Home() {
  const { role, incidents, loading, error, reload, logout } = useApp();
  const [search, setSearch] = useState('');
  const [logoutError, setLogoutError] = useState('');
  const filtered = incidents.filter(item => `${item.title} ${item.address} ${item.neighborhood} ${item.category}`.toLocaleLowerCase('pt-BR').includes(search.trim().toLocaleLowerCase('pt-BR')));

  if (role === 'GESTOR') return <Redirect href="/gestor" />;
  return <View style={ui.page}><FlatList data={error ? [] : filtered} keyExtractor={item => item.id} contentContainerStyle={ui.content} keyboardShouldPersistTaps="handled"
    ListHeaderComponent={<View style={{ gap: 24 }}>
      <View style={ui.hero}><Text style={{ color: colors.mint, fontSize: 10, letterSpacing: 2, fontWeight: '700' }}>SEU BAIRRO, NOSSO CUIDADO</Text><Text style={ui.heroTitle}>Pequenas atitudes.{'\n'}Uma cidade mais preparada.</Text><Text style={ui.heroCopy}>Viu um bueiro obstruído ou um ponto de alagamento? Registre o local e ajude a manter os riscos no radar.</Text><Action title="+  Registrar uma ocorrência" disabled={loading || !!error} onPress={() => router.push('/nova-ocorrencia')} /></View>
      <View style={{ flexDirection: 'row', justifyContent: 'space-between', alignItems: 'center', gap: 12 }}><View style={{ gap: 4 }}><Text style={[ui.heading, { fontSize: 25 }]}>Ocorrências</Text><Text style={ui.subtitle}>Os pontos de atenção que você registrou.</Text></View><View style={{ minWidth: 46, height: 46, borderRadius: 14, backgroundColor: '#E0EEEF', alignItems: 'center', justifyContent: 'center' }}><Text style={{ color: colors.primary, fontSize: 20, fontWeight: '700' }}>{error ? '—' : incidents.length}</Text></View></View>
      <Field label="Buscar ocorrências" placeholder="Título, bairro, endereço ou categoria" value={search} onChangeText={setSearch} />
      <Text style={ui.eyebrow}>{filtered.length} REGISTRO(S) ENCONTRADO(S)</Text>
      {loading ? <ActivityIndicator color={colors.primary} /> : null}
      {error ? <><Text accessibilityRole="alert" style={ui.error}>{error}</Text><Action title="Tentar novamente" onPress={() => { void reload(); }} /></> : null}
    </View>}
    renderItem={({ item }) => <Pressable accessibilityRole="button" accessibilityLabel={`Consultar ${item.title}`} onPress={() => router.push({ pathname: '/detalhe/[id]', params: { id: item.id } })} style={({ pressed }) => [ui.card, { borderLeftWidth: 4, borderLeftColor: colors.primary, backgroundColor: pressed ? '#F1FAF7' : '#FFFFFF' }]}>
      <View style={{ flexDirection: 'row', justifyContent: 'space-between', flexWrap: 'wrap', gap: 10 }}><Text style={ui.eyebrow}>{item.category.toLocaleUpperCase('pt-BR')}</Text><View style={ui.badge}><Text style={ui.badgeText}>●  {statusLabels[item.status]}</Text></View></View>
      <Text style={{ fontSize: 20, lineHeight: 27, fontWeight: '600', color: colors.ink }}>{item.title}</Text>
      <Text style={ui.subtitle}>{item.neighborhood} · {item.address}</Text>
      <View style={{ borderTopWidth: 1, borderTopColor: colors.border, paddingTop: 14, flexDirection: 'row', justifyContent: 'space-between', gap: 12 }}><Text style={{ color: colors.muted, fontSize: 12 }}>{new Date(item.createdAt).toLocaleDateString('pt-BR')}</Text><Text style={{ color: colors.primary, fontWeight: '700', fontSize: 13 }}>Ver detalhes  →</Text></View>
    </Pressable>}
    ListEmptyComponent={!loading && !error ? <View style={ui.card}><Text style={ui.label}>{search ? 'Nenhum resultado por aqui.' : 'Todo cuidado começa com um olhar.'}</Text><Text style={ui.subtitle}>{search ? 'Tente outro título, bairro ou categoria.' : 'Quando identificar um ponto de atenção, registre sua primeira ocorrência.'}</Text></View> : undefined}
    ListFooterComponent={<View style={{ gap: 16, marginTop: 8 }}>{logoutError ? <Text accessibilityRole="alert" style={ui.error}>{logoutError}</Text> : null}<Action title="Sair da conta" secondary onPress={() => { void logout().catch(() => setLogoutError('Não foi possível sair. Tente novamente.')); }} /><Text style={{ textAlign: 'center', color: colors.muted, fontSize: 12 }}>ANTES DA CHUVA · Cuidado que antecipa.</Text></View>}
  /></View>;
}
