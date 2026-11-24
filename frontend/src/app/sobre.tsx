import Constants from 'expo-constants';
import { router } from 'expo-router';
import { Text, View } from 'react-native';
import { Action, CityIllustration, Page, ui } from '../components/incident-ui';
export default function About() {
  return <Page><Text style={ui.eyebrow}>GLOBAL SOLUTION 2026/2</Text><Text style={ui.heading}>Antes da Chuva</Text>
    <Text style={ui.subtitle}>Zeladoria preventiva para uma cidade mais preparada. Registre riscos no bairro e acompanhe o atendimento e a triagem por inteligência artificial.</Text>
    <CityIllustration />
    <View style={ui.card}><Text style={ui.label}>Versão</Text><Text style={ui.subtitle}>{Constants.expoConfig?.version}</Text>
      <Text style={ui.label}>Commit de referência deste build</Text><Text selectable style={ui.subtitle}>{Constants.expoConfig?.extra?.buildCommit ?? 'Não disponível'}</Text></View>
    <Action title="Voltar ao perfil" secondary onPress={() => router.replace('/perfil')} />
  </Page>;
}
