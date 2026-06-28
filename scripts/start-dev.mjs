import { existsSync, mkdirSync, openSync, readFileSync, readdirSync, statSync } from 'node:fs';
import { dirname, resolve } from 'node:path';
import { spawn } from 'node:child_process';
import { fileURLToPath } from 'node:url';

const scriptDir = dirname(fileURLToPath(import.meta.url));
const projectRoot = resolve(scriptDir, '..');
const logsDir = resolve(projectRoot, 'logs');

mkdirSync(logsDir, { recursive: true });

function loadDotEnv() {
  const envFile = resolve(projectRoot, '.env');
  if (!existsSync(envFile)) {
    return {};
  }
  const result = {};
  for (const rawLine of readFileSync(envFile, 'utf8').split(/\r?\n/)) {
    const line = rawLine.trim();
    if (!line || line.startsWith('#')) {
      continue;
    }
    const separator = line.indexOf('=');
    if (separator < 1) {
      continue;
    }
    const key = line.slice(0, separator).trim();
    let value = line.slice(separator + 1).trim();
    if ((value.startsWith('"') && value.endsWith('"')) || (value.startsWith("'") && value.endsWith("'"))) {
      value = value.slice(1, -1);
    }
    if (key && !process.env[key]) {
      result[key] = value;
    }
  }
  return result;
}

function start(name, command, args, cwd = projectRoot, extraEnv = {}) {
  const out = openSync(resolve(logsDir, `${name}.log`), 'a');
  const err = openSync(resolve(logsDir, `${name}.err.log`), 'a');
  const child = spawn(command, args, {
    cwd,
    detached: true,
    windowsHide: true,
    stdio: ['ignore', out, err],
    env: normalizeEnv({ ...process.env, ...loadDotEnv(), ...extraEnv })
  });
  child.unref();
  return child.pid;
}

function normalizeEnv(source) {
  const result = {};
  const seen = new Set();
  for (const [key, value] of Object.entries(source)) {
    const normalized = key.toLowerCase();
    if (seen.has(normalized)) {
      continue;
    }
    seen.add(normalized);
    result[normalized === 'path' ? 'Path' : key] = value;
  }
  return result;
}

const loadedEnv = loadDotEnv();
const runtimeEnv = normalizeEnv({ ...process.env, ...loadedEnv });

function requireEnv(keys) {
  const missing = keys.filter((key) => !runtimeEnv[key] || !String(runtimeEnv[key]).trim());
  if (missing.length > 0) {
    throw new Error(
      `Missing required SQL Server environment variable(s): ${missing.join(', ')}. ` +
        'Copy .env.example to .env and fill in local values.'
    );
  }
}

function localJavaExe() {
  const localJdkRoot = resolve(projectRoot, '.tools', 'temurin-jdk-21');
  if (!existsSync(localJdkRoot)) {
    return null;
  }
  const candidates = readdirSync(localJdkRoot)
    .map((name) => resolve(localJdkRoot, name, 'bin', 'java.exe'))
    .filter((candidate) => existsSync(candidate))
    .sort((a, b) => statSync(b).mtimeMs - statSync(a).mtimeMs);
  return candidates[0] ?? null;
}

requireEnv(['DB_URL', 'DB_USERNAME', 'DB_PASSWORD']);

const javaExe = process.env.JAVA_HOME ? resolve(process.env.JAVA_HOME, 'bin', 'java.exe') : localJavaExe() ?? 'java';
const nodeExe = process.execPath;
const backendJar = resolve(projectRoot, 'target', 'claimops-0.0.1-SNAPSHOT.jar');
const viteBin = resolve(projectRoot, 'frontend', 'node_modules', 'vite', 'bin', 'vite.js');

const backendPid = start('backend', javaExe, ['-jar', backendJar], projectRoot, {
  SPRING_PROFILES_ACTIVE: 'sqlserver'
});
const frontendPid = start('frontend', nodeExe, [viteBin, '--host', '127.0.0.1', '--port', '5173'], resolve(projectRoot, 'frontend'));

console.log(`Spring Boot API starting with SQL Server on http://localhost:8080 (pid ${backendPid})`);
console.log(`React dashboard starting on http://127.0.0.1:5173 (pid ${frontendPid})`);
