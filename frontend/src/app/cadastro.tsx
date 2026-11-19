import { useState } from 'react';
import { router } from 'expo-router';
import { Text } from 'react-native';
import { Action, Field, Page, ui } from '../components/incident-ui';
import { useApp } from '../context/AppContext';
export default function Register() {
  const { register } = useApp();
  const [name, setName] = useState('');
  const [email, setEmail] = useState('');
  const [password, setPassword] = useState('');
  const [neighborhood, setNeighborhood] = useState('');
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState('');
  async function submit() {
    if (busy) return;
    if (!name.trim() || !/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(email.trim()) || password.length < 8 || password.length > 72) {
      setError('Informe nome, e-mail válido e senha entre 8 e 72 caracteres.'); return;
    }
    setBusy(true); setError('');
    try { await register({ name, email, password, neighborhood }); }
    catch (error) { setError(error instanceof Error ? error.message : 'Não foi possível criar sua conta.'); }
    finally { setBusy(false); }
  }
  return <Page><Text style={ui.eyebrow}>FAÇA PARTE</Text><Text style={ui.heading}>Crie sua conta.</Text>
    <Field label="Nome" value={name} onChangeText={setName} maxLength={100} editable={!busy} />
    <Field label="E-mail" value={email} onChangeText={setEmail} maxLength={150} autoCapitalize="none" keyboardType="email-address" editable={!busy} />
    <Field label="Senha (8 a 72 caracteres)" value={password} onChangeText={setPassword} maxLength={72} secureTextEntry editable={!busy} />
    <Field label="Bairro (opcional)" value={neighborhood} onChangeText={setNeighborhood} maxLength={80} editable={!busy} />
    {error ? <Text accessibilityRole="alert" style={ui.error}>{error}</Text> : null}
    <Action title={busy ? 'Criando conta…' : 'Criar conta'} disabled={busy} onPress={() => { void submit(); }} />
    <Action title="Voltar ao login" secondary disabled={busy} onPress={() => router.replace('/login')} />
  </Page>;
}
