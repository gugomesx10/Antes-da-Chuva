const fs = require('node:fs');
const path = require('node:path');
const vm = require('node:vm');
const assert = require('node:assert/strict');
const root = process.argv[2] || path.resolve(__dirname, '..');
const ts = require(path.join(root, 'node_modules/typescript'));
const memory = new Map();
let failWrites = false;
const storage = {
  getItem: async key => memory.get(key) ?? null,
  setItem: async (key, value) => { if (failWrites) throw Error('Disk unavailable'); memory.set(key, value); },
  removeItem: async key => memory.delete(key),
};
function load(relative) {
  const source = fs.readFileSync(path.join(root, 'src', relative + '.ts'), 'utf8');
  const compiled = ts.transpileModule(source, { compilerOptions: { module: ts.ModuleKind.CommonJS } }).outputText;
  const exports = {};
  vm.runInNewContext(compiled, { exports, require: name => name.includes('async-storage') ? storage : load('types/incident') });
  return exports;
}
(async () => {
  const service = load('services/storage');
  const first = await service.getIncidents();
  assert.equal(first.length, 2);
  await assert.rejects(service.updateIncidentStatus(first[0].id, 'RESOLVIDA'));
  await service.saveSession({ email: 'teste@antesdachuva.com', role: 'CIDADAO' });
  await assert.rejects(service.updateIncidentStatus(first[0].id, 'RESOLVIDA'));
  await service.saveSession({ email: 'gestor@antesdachuva.com', role: 'GESTOR' });
  assert.equal((await service.getSession()).role, 'GESTOR');
  await service.updateIncidentStatus(first[0].id, 'EM_ANALISE');
  assert.equal((await service.getIncidentById(first[0].id)).status, 'EM_ANALISE');
  await assert.rejects(service.updateIncidentStatus(first[0].id, 'INVALID'));
  await assert.rejects(service.updateIncidentStatus('missing', 'RESOLVIDA'));
  failWrites = true;
  await assert.rejects(service.updateIncidentStatus(first[0].id, 'RESOLVIDA'));
  assert.equal((await service.getIncidentById(first[0].id)).status, 'EM_ANALISE');
  failWrites = false;
  const original = await service.getIncidentById(first[0].id);
  const input = { title: 'Título atualizado', address: 'Rua Atualizada, 20', neighborhood: 'Centro', description: 'Descrição atualizada para testar edição.', category: 'Entulho' };
  await assert.rejects(service.updateIncident(original.id, input));
  await assert.rejects(service.deleteIncident(original.id));
  await assert.rejects(service.saveIncident(input));
  const anotherId = first[1].id;
  await service.manageIncident(anotherId, { status: 'RESOLVIDA', note: ' ' });
  const resolved = await service.getIncidentById(anotherId);
  assert.equal(resolved.status, 'RESOLVIDA');
  assert.equal(resolved.priority, undefined);
  assert.equal(resolved.updates[0].note, '');
  await service.manageIncident(anotherId, { status: 'RESOLVIDA' });
  assert.equal((await service.getIncidentById(anotherId)).updates.length, 1);
  await service.manageIncident(anotherId, { status: 'RESOLVIDA', note: 'OK' });
  assert.equal((await service.getIncidentById(anotherId)).updates[0].note, 'OK');
  const longNote = 'a'.repeat(1001);
  await service.manageIncident(anotherId, { status: 'RESOLVIDA', note: longNote });
  assert.equal((await service.getIncidentById(anotherId)).updates[0].note, longNote);
  const managed = await service.manageIncident(original.id, { status: 'EM_ANDAMENTO', priority: 'ALTA', note: 'Equipe direcionada para avaliar o local.' });
  assert.equal(managed.title, original.title);
  assert.equal(managed.address, original.address);
  assert.equal(managed.description, original.description);
  assert.equal(managed.priority, 'ALTA');
  assert.equal((await service.getIncidentById(original.id)).updates[0].note, 'Equipe direcionada para avaliar o local.');
  await service.saveSession({ email: 'teste@antesdachuva.com', role: 'CIDADAO' });
  await assert.rejects(service.manageIncident(original.id, { status: 'RESOLVIDA', priority: 'BAIXA', note: 'Tentativa do cidadão.' }));
  const edited = await service.updateIncident(original.id, input);
  assert.equal(edited.title, input.title);
  assert.equal(edited.createdAt, original.createdAt);
  assert.equal(edited.status, 'EM_ANDAMENTO');
  assert.equal(edited.priority, 'ALTA');
  assert.equal(edited.updates[0].note, managed.updates[0].note);
  assert.equal((await service.getIncidentById(original.id)).address, input.address);
  await assert.rejects(service.updateIncident(original.id, { ...input, title: '' }));
  await assert.rejects(service.updateIncident('missing', input));
  await assert.rejects(service.deleteIncident('missing'));
  failWrites = true;
  await assert.rejects(service.updateIncident(original.id, { ...input, title: 'Não deve gravar' }));
  await assert.rejects(service.deleteIncident(original.id));
  assert.equal((await service.getIncidentById(original.id)).title, input.title);
  failWrites = false;
  await service.clearSession();
  await assert.rejects(service.updateIncident(original.id, input));
  await assert.rejects(service.deleteIncident(original.id));
  await service.saveSession({ email: 'teste@antesdachuva.com', role: 'CIDADAO' });
  await service.updateIncident(original.id, input);
  for (const item of await service.getIncidents()) await service.deleteIncident(item.id);
  assert.equal((await service.getIncidents()).length, 0, 'Deleting the last item must not recreate samples');
  assert.equal(await service.getIncidentById(original.id), undefined);
  await service.clearSession();
  assert.equal(await service.getSession(), null);
  memory.set('@antes-da-chuva/session:v1', JSON.stringify({email:'teste@antesdachuva.com'}));
  assert.equal((await service.getSession()).role, 'CIDADAO');
  memory.set('@antes-da-chuva/incidents:v1', JSON.stringify([{ ...first[0], address: undefined, neighborhood: undefined, location: 'Endereço antigo' }]));
  const migrated = (await service.getIncidents())[0];
  assert.equal(migrated.address, 'Endereço antigo');
  assert.equal(migrated.neighborhood, 'Não informado');
  const key = '@antes-da-chuva/incidents:v1';
  const legacyIncident = {
    id: 'legacy-priority', title: 'Bueiro com acúmulo de folhas',
    location: 'Rua das Flores, 120 — Centro', category: 'Bueiro obstruído',
    description: 'Folhas impedem o escoamento de água.',
    priority: 'Média', createdAt: '2026-11-17T12:00:00.000Z',
  };
  const legacyData = JSON.stringify([
    legacyIncident,
    { ...first[1], priority: null, updates: [{
      id: 'legacy-update', status: 'EM_ANALISE', priority: 'Alta',
      createdAt: '2026-11-17T13:00:00.000Z',
    }] },
  ]);
  memory.set(key, legacyData);
  const recovered = await service.getIncidents();
  assert.equal(recovered.length, 2);
  assert.equal(recovered[0].priority, 'MEDIA');
  assert.equal(recovered[0].address, legacyIncident.location);
  assert.equal(recovered[1].priority, undefined);
  assert.equal(recovered[1].updates[0].priority, 'ALTA');
  assert.equal(recovered[1].updates[0].note, '');
  assert.equal(memory.get(key), legacyData, 'Reading must not rewrite existing data');

  await service.saveSession({ email: 'gestor@antesdachuva.com', role: 'GESTOR' });
  await service.manageIncident(legacyIncident.id, { status: 'EM_ANALISE', priority: 'MEDIA' });
  const afterSaving = await service.getIncidents();
  assert.equal(afterSaving.length, 2);
  assert.equal(afterSaving[0].updates[0].status, 'EM_ANALISE');
  assert.equal(afterSaving[1].updates[0].id, 'legacy-update');

  const invalidData = '[malformed';
  memory.set(key, invalidData);
  await assert.rejects(service.getIncidents());
  await assert.rejects(service.manageIncident(legacyIncident.id, { status: 'RESOLVIDA' }));
  assert.equal(memory.get(key), invalidData, 'An unreadable store must not be overwritten');
  console.log('PASS: roles, denied access, status persistence, validation, write failure, logout, legacy records and priorities, safe reads, edit validation, preserved metadata, delete, empty list and write failures');
})().catch(error => { console.error(error); process.exitCode = 1; });
