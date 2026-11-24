// Opt-in integration test: creates a test citizen and cancels its test occurrence.
// Exercises the actual mobile services against the configured Java deployment.
const assert = require('node:assert/strict');
const fs = require('node:fs');
const Module = require('node:module');
const ts = require('typescript');
const memory = new Map();
const originalLoad = Module._load;
Module._load = function (name, ...args) {
  if (name === '@react-native-async-storage/async-storage') return {
    getItem: async key => memory.get(key) ?? null,
    setItem: async (key, value) => { memory.set(key, value); },
    multiRemove: async keys => keys.forEach(key => memory.delete(key)),
  };
  return originalLoad.call(this, name, ...args);
};
require.extensions['.ts'] = (module, filename) => module._compile(ts.transpileModule(fs.readFileSync(filename, 'utf8'), { compilerOptions: { module: ts.ModuleKind.CommonJS, target: ts.ScriptTarget.ES2022, esModuleInterop: true } }).outputText, filename);
if (!process.env.EXPO_PUBLIC_API_URL) throw new Error('Set EXPO_PUBLIC_API_URL explicitly to run the live test.');
const { authService } = require('../src/services/authService.ts');
const service = require('../src/services/incidentService.ts');
const { api, configureSession } = require('../src/services/api.ts');
async function main() {
  const unique = `${Date.now()}${Math.floor(Math.random() * 10000)}`;
  const email = `mobile.qa.${unique}@example.com`;
  const password = require('node:crypto').randomBytes(18).toString('hex');
  let id;
  try {
    await assert.rejects(() => api('/auth/me'), error => error.status === 401);
    const session = await authService.register({ name: 'Validação Mobile Codex', email, password, neighborhood: 'Teste de integração' });
    assert.equal(session.user.role, 'CIDADAO');
    assert.equal((await authService.me()).email, email);
    await authService.clear();
    await authService.login(email, password);
    const categories = await service.getCategories();
    const category = categories.find(value => value.active);
    assert.ok(category);
    const input = { title: `Teste integração mobile ${unique}`, description: 'Registro automatizado para validar integração. Pode ser ignorado na operação.', address: 'Rua de teste, 100', neighborhood: 'Teste de integração', category: category.name, categoryId: category.id };
    const created = await service.saveIncident(input, categories); id = created.id;
    assert.equal(created.status, 'ABERTA');
    assert.ok((await service.getIncidents('CIDADAO', categories)).some(value => value.id === id));
    assert.equal((await service.getIncidentById(id, categories)).title, input.title);
    const updated = await service.updateIncident(id, { ...input, title: 'Teste mobile atualizado' }, categories);
    assert.equal(updated.title, 'Teste mobile atualizado');
    await service.deleteIncident(id);
    assert.equal((await service.getIncidentById(id, categories)).status, 'CANCELADA');
    await assert.rejects(() => service.updateIncident(id, input, categories), error => error.status === 409);
    await authService.clear();
    configureSession(null);
    await assert.rejects(() => api('/incidents/mine'), error => error.status === 401);
    assert.equal(memory.size, 0);
    console.log('PASS: unauthorized, signup, JWT login/me, categories, create/list/detail/update/cancel, conflict, logout.');
    console.log(`Test citizen: ${email}; cancelled occurrence: ${id}. No account deletion endpoint exists.`);
  } catch (error) {
    if (id) await service.deleteIncident(id).catch(() => {});
    throw error;
  }
}
main().catch(error => { console.error(error.message); process.exitCode = 1; });
