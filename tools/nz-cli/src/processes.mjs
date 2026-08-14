import { spawn, spawnSync } from 'node:child_process'
import path from 'node:path'
import { CliError } from './errors.mjs'

function mavenCommand(root) {
  return process.platform === 'win32'
    ? path.join(root, 'nz-server', 'mvnw.cmd')
    : path.join(root, 'nz-server', 'mvnw')
}

function commandSpec(root, kind, args) {
  if (kind === 'maven') {
    return { command: mavenCommand(root), args, cwd: path.join(root, 'nz-server') }
  }
  if (kind === 'cli') {
    return { command: process.execPath, args, cwd: path.join(root, 'tools', 'nz-cli') }
  }
  return { command: process.platform === 'win32' ? 'pnpm.cmd' : 'pnpm', args, cwd: path.join(root, 'nz-web') }
}

export function runChecked(spec, spawnSyncImpl = spawnSync) {
  const result = spawnSyncImpl(spec.command, spec.args, {
    cwd: spec.cwd,
    env: process.env,
    shell: process.platform === 'win32',
    stdio: 'inherit',
  })
  if (result.error) throw new CliError(`无法启动 ${spec.command}：${result.error.message}`, 1)
  if (result.status !== 0) {
    throw new CliError(`命令执行失败（${result.status ?? 'unknown'}）：${spec.command} ${spec.args.join(' ')}`, 1)
  }
}

export function buildProject(root, options = {}, spawnSyncImpl = spawnSync) {
  const runBackend = !options.frontendOnly
  const runFrontend = !options.backendOnly
  if (runBackend) {
    runChecked(commandSpec(root, 'maven', ['-pl', 'nz-app', '-am', 'package', '-DskipTests']), spawnSyncImpl)
  }
  if (runFrontend) runChecked(commandSpec(root, 'pnpm', ['build']), spawnSyncImpl)
}

export function verifyProject(root, options = {}, spawnSyncImpl = spawnSync) {
  const runBackend = !options.frontendOnly
  const runFrontend = !options.backendOnly
  const runCli = !options.backendOnly && !options.frontendOnly
  if (runBackend) runChecked(commandSpec(root, 'maven', ['-pl', 'nz-app', '-am', 'test']), spawnSyncImpl)
  if (runFrontend) {
    runChecked(commandSpec(root, 'pnpm', ['test']), spawnSyncImpl)
    runChecked(commandSpec(root, 'pnpm', ['build']), spawnSyncImpl)
  }
  if (runCli) runChecked(commandSpec(root, 'cli', ['--test']), spawnSyncImpl)
}

export function devProject(root, options = {}, implementations = {}) {
  const spawnImpl = implementations.spawn || spawn
  const spawnSyncImpl = implementations.spawnSync || spawnSync
  if (!options.noPrepare) {
    runChecked(commandSpec(root, 'maven', ['-pl', 'nz-app', '-am', 'install', '-DskipTests']), spawnSyncImpl)
  }

  const backend = commandSpec(root, 'maven', ['-pl', 'nz-app', 'spring-boot:run'])
  const frontend = commandSpec(root, 'pnpm', ['dev'])
  const children = [backend, frontend].map((spec) =>
    spawnImpl(spec.command, spec.args, {
      cwd: spec.cwd,
      env: process.env,
      shell: process.platform === 'win32',
      stdio: 'inherit',
    }),
  )

  let stopping = false
  const stop = (signal = 'SIGTERM') => {
    if (stopping) return
    stopping = true
    for (const child of children) {
      if (!child.killed) child.kill(signal)
    }
  }
  for (const signal of ['SIGINT', 'SIGTERM']) process.once(signal, () => stop(signal))
  for (const child of children) {
    child.once('exit', (code) => {
      if (!stopping && code !== 0) {
        stop()
        process.exitCode = code || 1
      }
    })
    child.once('error', (error) => {
      stop()
      process.exitCode = 1
      console.error(`开发进程启动失败：${error.message}`)
    })
  }
  return { children, stop }
}

export function processCommandSpec(root, kind, args) {
  return commandSpec(root, kind, args)
}
