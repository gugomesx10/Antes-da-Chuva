import { useState, type PropsWithChildren } from 'react';
import { KeyboardAvoidingView, Platform, Pressable, ScrollView, StyleSheet, Text, TextInput, View, type TextInputProps } from 'react-native';

export const colors = {
  background: '#F3F6F8', ink: '#102D42', muted: '#52697A', primary: '#087D80',
  border: '#DBE4EA', navy: '#102D42', mint: '#C9F0E4', yellow: '#F5CE75',
};
export const ui = StyleSheet.create({
  page: { flex: 1, backgroundColor: colors.background },
  content: { width: '100%', maxWidth: 760, alignSelf: 'center', padding: 24, paddingBottom: 48, gap: 20 },
  heading: { fontSize: 32, lineHeight: 39, fontWeight: '700', letterSpacing: -1, color: colors.ink },
  subtitle: { fontSize: 15, lineHeight: 24, color: colors.muted },
  label: { fontSize: 14, fontWeight: '600', color: colors.ink },
  card: { padding: 24, backgroundColor: '#FFFFFF', borderRadius: 20, borderWidth: 1, borderColor: colors.border, gap: 14 },
  error: { color: '#A12626', fontSize: 14, lineHeight: 22 },
  row: { flexDirection: 'row', flexWrap: 'wrap', gap: 8 },
  eyebrow: { color: colors.primary, fontSize: 11, fontWeight: '700', letterSpacing: 2 },
  hero: { backgroundColor: colors.navy, borderRadius: 24, padding: 28, gap: 18, overflow: 'hidden' },
  heroTitle: { fontSize: 33, lineHeight: 40, letterSpacing: -1, fontWeight: '700', color: '#FFFFFF' },
  heroCopy: { fontSize: 15, lineHeight: 24, color: '#C4D8E3', maxWidth: 480 },
  badge: { alignSelf: 'flex-start', paddingHorizontal: 10, paddingVertical: 6, borderRadius: 7, backgroundColor: '#E5F4EE' },
  badgeText: { fontSize: 10, fontWeight: '700', letterSpacing: 1, color: '#19634F' },
});

export function Brand({ light = false }: { light?: boolean }) {
  return <View style={{ flexDirection: 'row', alignItems: 'center', gap: 12 }}>
    <View accessibilityElementsHidden importantForAccessibility="no-hide-descendants" style={{ width: 42, height: 46, alignItems: 'center', justifyContent: 'center' }}>
      <View style={{ position: 'absolute', width: 32, height: 32, borderTopLeftRadius: 4, borderTopRightRadius: 20, borderBottomLeftRadius: 20, borderBottomRightRadius: 20, backgroundColor: light ? colors.mint : colors.primary, transform: [{ rotate: '45deg' }] }} />
      <View style={{ flexDirection: 'row', alignItems: 'flex-end', gap: 2, marginTop: 8 }}>
        {[10, 17, 13].map((height, index) => <View key={index} style={{ height, width: 5, borderRadius: 1, backgroundColor: light ? colors.navy : '#FFFFFF' }} />)}
      </View>
    </View>
    <View><Text style={{ fontSize: 18, fontWeight: '700', letterSpacing: -0.5, color: light ? '#FFFFFF' : colors.ink }}>Antes da Chuva</Text><Text style={{ fontSize: 10, letterSpacing: 1.8, marginTop: 3, color: light ? '#B6D5DF' : colors.muted }}>CUIDADO QUE ANTECIPA.</Text></View>
  </View>;
}

export function CityIllustration() {
  return <View accessible={false} style={{ height: 124, flexDirection: 'row', alignItems: 'flex-end', gap: 9, paddingHorizontal: 12, borderBottomWidth: 2, borderBottomColor: '#6BB8B0', marginTop: 4 }}>
    <View style={{ position: 'absolute', right: 20, top: 0, width: 44, height: 44, borderRadius: 22, backgroundColor: colors.yellow }} />
    {[48, 76, 56, 102, 70, 88, 46].map((height, index) => <View key={index} style={{ flex: 1, height, borderTopLeftRadius: 5, borderTopRightRadius: 5, backgroundColor: index === 3 ? '#6BB8B0' : '#254D61', padding: 8, gap: 8 }}>
      {[0, 1, 2].map(row => <View key={row} style={{ flexDirection: 'row', gap: 5 }}><View style={{ width: 4, height: 5, backgroundColor: '#9CCBC8' }} /><View style={{ width: 4, height: 5, backgroundColor: '#9CCBC8' }} /></View>)}
    </View>)}
  </View>;
}

export function Page({ children }: PropsWithChildren) {
  return <KeyboardAvoidingView style={ui.page} behavior={Platform.OS === 'ios' ? 'padding' : undefined}><ScrollView keyboardShouldPersistTaps="handled" contentContainerStyle={ui.content}>{children}</ScrollView></KeyboardAvoidingView>;
}
export function Action({ title, onPress, disabled = false, secondary = false }: { title: string; onPress: () => void; disabled?: boolean; secondary?: boolean }) {
  return <Pressable accessibilityRole="button" accessibilityState={{ disabled }} disabled={disabled} onPress={onPress} style={({ pressed }) => ({ minHeight: 52, justifyContent: 'center', padding: 16, borderRadius: 12, borderWidth: secondary ? 1 : 0, borderColor: colors.border, backgroundColor: secondary ? '#FFFFFF' : pressed ? '#06676A' : colors.primary, opacity: disabled ? 0.5 : 1 })}><Text style={{ textAlign: 'center', fontSize: 15, fontWeight: '700', color: secondary ? colors.ink : '#FFFFFF' }}>{title}</Text></Pressable>;
}
export function Field({ label, ...props }: TextInputProps & { label: string }) {
  const [focused, setFocused] = useState(false);
  return <View style={{ gap: 8 }}><Text style={ui.label}>{label}</Text><TextInput accessibilityLabel={label} placeholderTextColor={colors.muted} {...props} onFocus={event => { setFocused(true); props.onFocus?.(event); }} onBlur={event => { setFocused(false); props.onBlur?.(event); }} style={[{ backgroundColor: focused ? '#F4FBFA' : '#FAFCFD', color: colors.ink, minHeight: 52, padding: 15, borderRadius: 12, borderWidth: 1, borderColor: focused ? colors.primary : colors.border, fontSize: 16 }, props.multiline && { minHeight: 120, textAlignVertical: 'top' }, props.style]} /></View>;
}
export function Choices<T extends string>({ label, values, value, onChange, disabled = false }: { label: string; values: readonly T[]; value: T; onChange: (value: T) => void; disabled?: boolean }) {
  return <View style={{ gap: 10 }}><Text style={ui.label}>{label}</Text><View style={ui.row}>{values.map(option => <Pressable key={option} accessibilityRole="radio" accessibilityState={{ checked: option === value, disabled }} disabled={disabled} onPress={() => onChange(option)} style={{ minHeight: 44, justifyContent: 'center', padding: 12, borderRadius: 10, borderWidth: 1, borderColor: option === value ? colors.primary : colors.border, backgroundColor: option === value ? '#E0F3EF' : '#FFFFFF' }}><Text style={{ fontSize: 14, fontWeight: option === value ? '700' : '400', color: option === value ? '#075F63' : colors.ink }}>{option === value ? '✓  ' : ''}{option}</Text></Pressable>)}</View></View>;
}
