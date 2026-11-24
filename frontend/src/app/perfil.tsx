import { useState } from 'react';
import { router } from 'expo-router';
import { Text, View } from 'react-native';
import { Action, Page, ui } from '../components/incident-ui';
import { useApp } from '../context/AppContext';
export default function Profile() {
  const { session, logout } = useApp();
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState('');
  async function leave() {
    setBusy(true);
    try { await logout(); }
    catch { setError('Não foi possível limpar a sessão armazenada. Tente novamente.'); }
    finally { setBusy(false); }
  }
  return <Page><Text style={ui.eyebrow}>MINHA CONTA</Text><Text style={ui.heading}>{session?.user.name}</Text>
    <View style={ui.card}><Text style={ui.label}>E-mail</Text><Text selectable style={ui.subtitle}>{session?.user.email}</Text>
      <Text style={ui.label}>Perfil</Text><Text style={ui.subtitle}>{session?.user.role === 'GESTOR' ? 'Gestor' : 'Cidadão'}</Text>
      <Text style={ui.label}>Bairro</Text><Text style={ui.subtitle}>{session?.user.neighborhood || 'Não informado'}</Text></View>
    {error ? <Text style={ui.error}>{error}</Text> : null}
    <Action title={busy ? 'Saindo…' : 'Sair da conta'} disabled={busy} onPress={() => { void leave(); }} />
    <Action title="Sobre o App" secondary onPress={() => router.push('/sobre')} />
    <Action title="Voltar às ocorrências" secondary onPress={() => router.replace('/')} />
  </Page>;
}
