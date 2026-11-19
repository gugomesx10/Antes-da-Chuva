import { router } from 'expo-router';
import { useState } from 'react';
import { ScrollView, Text, View, useWindowDimensions, KeyboardAvoidingView, Platform } from 'react-native';
import { Action, CityIllustration, colors, Field, ui } from '../components/incident-ui';
import { useApp } from '../context/AppContext';

export default function Login() {
  const { login } = useApp();
  const wide = useWindowDimensions().width >= 860;
  const [busy, setBusy] = useState(false);
  const [email, setEmail] = useState('');
  const [password, setPassword] = useState('');
  const [error, setError] = useState('');

  async function submit() {
    if (busy) return;
    if (!email.trim() || !password) { setError('Preencha e-mail e senha.'); return; }
    setBusy(true); setError('');
    try { if (!await login(email, password)) setError('E-mail ou senha inválidos.'); }
    catch (error) { setError(error instanceof Error ? error.message : 'Não foi possível entrar.'); }
    finally { setBusy(false); }
  }

  return <KeyboardAvoidingView style={ui.page} behavior={Platform.OS === 'ios' ? 'padding' : undefined}>
    <ScrollView keyboardShouldPersistTaps="handled" contentContainerStyle={{ flexGrow: 1, justifyContent: 'center', padding: wide ? 48 : 20 }}>
      <View style={{ width: '100%', maxWidth: 1080, alignSelf: 'center', flexDirection: wide ? 'row' : 'column', gap: wide ? 0 : 24, backgroundColor: wide ? '#FFFFFF' : 'transparent', borderRadius: 28, overflow: 'hidden', borderWidth: wide ? 1 : 0, borderColor: colors.border }}>
        <View style={{ flex: 1, backgroundColor: colors.navy, borderRadius: wide ? 0 : 24, padding: wide ? 44 : 28, justifyContent: 'space-between', gap: 36 }}>
          <View style={{ gap: 18 }}><Text style={{ color: colors.mint, letterSpacing: 2, fontSize: 10, fontWeight: '700' }}>ZELADORIA PREVENTIVA</Text>
            <Text style={[ui.heroTitle, { fontSize: wide ? 46 : 34, lineHeight: wide ? 53 : 41 }]}>O melhor momento{wide ? '\n' : ' '}para cuidar é{wide ? '\n' : ' '}antes da chuva.</Text>
            <Text style={ui.heroCopy}>Um olhar atento à sua rua pode fazer a diferença. Registre riscos e acompanhe os cuidados com o seu bairro.</Text>
          </View>
          <CityIllustration />
          <Text style={{ color: '#B6D5DF', fontSize: 12 }}>Pessoas atentas. Cidades mais preparadas.</Text>
        </View>
        <View style={{ flex: 1, padding: wide ? 44 : 4, justifyContent: 'center', gap: 24 }}>
          <View style={{ gap: 10 }}><Text style={ui.eyebrow}>BEM-VINDO AO ANTES DA CHUVA</Text><Text style={ui.heading}>Cuidar começa{wide ? '\n' : ' '}por aqui.</Text><Text style={ui.subtitle}>Entre para registrar e consultar pontos de atenção na sua comunidade.</Text></View>
          <View style={{ gap: 18 }}>
            <Field label="E-mail" placeholder="Seu endereço de e-mail" value={email} onChangeText={setEmail} keyboardType="email-address" autoCapitalize="none" autoComplete="email" maxLength={120} editable={!busy} />
            <Field label="Senha" placeholder="Sua senha" value={password} onChangeText={setPassword} secureTextEntry autoCapitalize="none" autoComplete="current-password" maxLength={100} editable={!busy} onSubmitEditing={() => { void submit(); }} />
            {error ? <Text accessibilityRole="alert" style={ui.error}>{error}</Text> : null}
            <Action title={busy ? 'Entrando…' : 'Entrar na minha conta  →'} disabled={busy} onPress={() => { void submit(); }} />
            <Action title="Criar conta" secondary disabled={busy} onPress={() => router.push('/cadastro')} />
          </View>

        </View>
      </View>
    </ScrollView>
  </KeyboardAvoidingView>;
}
