import { useRef, useState } from 'react';
import { router } from 'expo-router';
import { Text } from 'react-native';
import { Action, Choices, Field, Page, ui } from './incident-ui';
import { useApp } from '../context/AppContext';
import { type Incident, type IncidentInput } from '../types/incident';
export default function IncidentForm({ initial, onSave }: { initial?: Incident; onSave: (input: IncidentInput) => Promise<unknown> }) {
  const { categories } = useApp();
  const availableCategories = categories.filter(value => value.active);
  const [title, setTitle] = useState(initial?.title ?? ''); const [address, setAddress] = useState(initial?.address ?? ''); const [description, setDescription] = useState(initial?.description ?? '');
  const [category, setCategory] = useState<Incident['category']>(initial?.category ?? availableCategories[0]?.name ?? ''); const [neighborhood, setNeighborhood] = useState(initial?.neighborhood ?? '');
  const [saving, setSaving] = useState(false); const [error, setError] = useState(''); const pending = useRef(false);
  async function submit() {
    if (pending.current) return;
    if (title.trim().length < 3 || address.trim().length < 5 || description.trim().length < 10 || neighborhood.trim().length < 2) { setError('Informe um título (mínimo 3 caracteres), endereço (mínimo 5), bairro (mínimo 2) e descrição (mínimo 10).'); return; }
    pending.current = true; setSaving(true); setError('');
    try { await onSave({ title, address, neighborhood, description, category, categoryId: availableCategories.find(value => value.name === category)?.id, latitude: initial?.latitude, longitude: initial?.longitude }); router.back(); }
    catch (error) { setError(error instanceof Error ? error.message : 'Não foi possível salvar. Seus campos foram mantidos.'); }
    finally { pending.current = false; setSaving(false); }
  }
  return <Page><Text style={ui.eyebrow}>{initial ? 'EDITAR OCORRÊNCIA' : 'NOVO PONTO DE ATENÇÃO'}</Text><Text style={ui.heading}>{initial ? 'Atualize os detalhes.' : 'Seu olhar faz a diferença.'}</Text><Text style={ui.subtitle}>Descreva o ponto de atenção com informações que facilitem sua identificação.</Text><Field label="Título" placeholder="Ex.: Bueiro obstruído na esquina" value={title} onChangeText={setTitle} maxLength={80} editable={!saving} /><Field label="Endereço" placeholder="Rua e número" value={address} onChangeText={setAddress} maxLength={180} editable={!saving} /><Choices label="Tipo de ocorrência" values={availableCategories.map(value => value.name)} disabled={saving} value={category} onChange={setCategory} /><Field label="Bairro" value={neighborhood} onChangeText={setNeighborhood} maxLength={80} editable={!saving} /><Field label="Descrição" multiline value={description} onChangeText={setDescription} maxLength={1000} editable={!saving} /><Text style={ui.subtitle}>{description.length}/1000 caracteres</Text>{error ? <Text accessibilityRole="alert" style={ui.error}>{error}</Text> : null}<Action title={saving ? 'Salvando…' : initial ? 'Salvar alterações' : 'Salvar ocorrência'} disabled={saving || !availableCategories.length} onPress={() => { void submit(); }} /><Action title="Cancelar" secondary disabled={saving} onPress={() => router.back()} /></Page>;
}
