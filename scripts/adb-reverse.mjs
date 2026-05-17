/**
 * Esegue `adb reverse tcp:<porta> tcp:<porta>` prima di `npm run dev`, così un device USB
 * può usare http://127.0.0.1:<porta>/ nell'app (vedi docs mobile).
 * Porta: `PORT` o `TSM_DEV_PORT` (default 3000, come in `backend/src/server.js`).
 */
import { spawnSync } from 'node:child_process'
import fs from 'node:fs'
import os from 'node:os'
import path from 'node:path'

const port = process.env.PORT ?? process.env.TSM_DEV_PORT ?? '3000'

function findAdb() {
  const exe = process.platform === 'win32' ? 'adb.exe' : 'adb'
  const fromEnv = process.env.ANDROID_HOME ?? process.env.ANDROID_SDK_ROOT
  if (fromEnv) {
    const p = path.join(fromEnv, 'platform-tools', exe)
    if (fs.existsSync(p)) return p
  }
  if (process.platform === 'win32' && process.env.LOCALAPPDATA) {
    const p = path.join(process.env.LOCALAPPDATA, 'Android', 'Sdk', 'platform-tools', exe)
    if (fs.existsSync(p)) return p
  }
  if (process.platform === 'darwin') {
    const p = path.join(os.homedir(), 'Library', 'Android', 'sdk', 'platform-tools', exe)
    if (fs.existsSync(p)) return p
  }
  const p = path.join(os.homedir(), 'Android', 'Sdk', 'platform-tools', exe)
  if (fs.existsSync(p)) return p
  return null
}

const adb = findAdb()
if (!adb) {
  console.warn('[adb-reverse] adb non trovato. Imposta ANDROID_HOME o installa Android SDK. Salto reverse.')
  process.exit(0)
}

function listUsbDevices(adbPath) {
  const r = spawnSync(adbPath, ['devices'], { encoding: 'utf8', windowsHide: true })
  if (r.status !== 0 || !r.stdout) return []
  const out = []
  for (const line of r.stdout.split('\n')) {
    const trimmed = line.trim()
    const m = trimmed.match(/^(\S+)\s+device$/)
    if (m) out.push(m[1])
  }
  return out
}

const serials = listUsbDevices(adb)
if (serials.length === 0) {
  console.warn('[adb-reverse] Nessun device in stato "device". Salto reverse.')
  process.exit(0)
}

let anyFailed = false
for (const serial of serials) {
  const r = spawnSync(adb, ['-s', serial, 'reverse', `tcp:${port}`, `tcp:${port}`], {
    stdio: 'inherit',
    windowsHide: true,
  })
  if (r.status !== 0) {
    anyFailed = true
    if (r.error) console.warn(`[adb-reverse] [${serial}]`, r.error.message)
  }
}

if (anyFailed) {
  console.warn(
    `[adb-reverse] reverse tcp:${port} fallito su almeno un device. Avvio server comunque.`,
  )
} else {
  console.log(`[adb-reverse] tcp:${port} → tcp:${port} (${serials.length} device)`)
}

process.exit(0)
