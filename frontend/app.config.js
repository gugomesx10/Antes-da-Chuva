const { execFileSync } = require('node:child_process');
module.exports = ({ config }) => {
  let buildCommit = process.env.EAS_BUILD_GIT_COMMIT_HASH || process.env.SOURCE_COMMIT;
  if (!buildCommit) {
    try { buildCommit = execFileSync('git', ['rev-parse', 'HEAD'], { cwd: __dirname, encoding: 'utf8' }).trim(); }
    catch { throw new Error('Defina SOURCE_COMMIT com o hash completo do código usado no build.'); }
  }
  if (!/^[a-f0-9]{40}$/i.test(buildCommit)) throw new Error('Hash de referência inválido.');
  return { ...config, android: { ...config.android, package: 'com.antesdachuva.mobile' }, extra: { ...config.extra, buildCommit } };
};
