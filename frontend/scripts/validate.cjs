const { spawn, spawnSync } = require('node:child_process');
const http = require('node:http');
const path = require('node:path');
const root = path.resolve(__dirname, '..');
const run = (file, args) => {
  const result = spawnSync(process.execPath, [path.join(root, 'node_modules', file), ...args], { cwd: root, stdio: 'inherit', env: { ...process.env, CI: '1', EXPO_OFFLINE: '1' } });
  if (result.status !== 0) process.exit(result.status || 1);
};
run('typescript/bin/tsc', ['--noEmit']);
run('eslint/bin/eslint.js', ['src', '--max-warnings=0']);
const server = spawn(process.execPath, [path.join(root, 'node_modules/expo/bin/cli'), 'start', '--web', '--offline', '--port', '8098'], { cwd: root, env: { ...process.env, CI: '1', EXPO_OFFLINE: '1' }, stdio: ['ignore', 'pipe', 'pipe'] });
server.stdout.on('data', data => process.stdout.write(data));
server.stderr.on('data', data => process.stderr.write(data));
const deadline = Date.now() + 90000;
let finished = false;
function finish(code) { if (finished) return; finished = true; server.kill(); process.exitCode = code; }
function probe() {
  if (finished) return;
  if (Date.now() > deadline) { console.error('Expo startup timeout'); finish(1); return; }
  const request = http.get('http://127.0.0.1:8098', response => {
    response.resume();
    if (response.statusCode === 200) { console.log('Expo HTTP startup: OK'); finish(0); }
    else setTimeout(probe, 1000);
  });
  request.on('error', () => setTimeout(probe, 1000));
  request.setTimeout(3000, () => request.destroy());
}
server.on('exit', code => { if (!finished) { console.error('Expo exited', code); finish(1); } });
probe();
