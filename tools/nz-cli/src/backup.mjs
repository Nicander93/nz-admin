import { cp, mkdir, readFile, rename, rm, rmdir, stat, writeFile } from 'node:fs/promises'
import path from 'node:path'
import { CliError } from './errors.mjs'
import { relativeDisplay } from './project.mjs'

function backupId(operation, now = new Date()) {
  const timestamp = now.toISOString().replace(/[-:.]/g, '')
  return `${timestamp}-${operation.replace(/[^a-z0-9-]+/gi, '-')}`
}

async function pathExists(file) {
  try {
    await stat(file)
    return true
  } catch {
    return false
  }
}

async function atomicWrite(file, content) {
  await mkdir(path.dirname(file), { recursive: true })
  const temporary = `${file}.nz-cli-${process.pid}-${Date.now()}.tmp`
  await writeFile(temporary, content, 'utf8')
  await rename(temporary, file)
}

export async function applyChanges(root, operation, changes, now = new Date()) {
  const normalized = [...changes.entries()].map(([file, content]) => [path.resolve(file), content])
  const id = backupId(operation, now)
  const directory = path.join(root, '.nz-cli', 'backups', id)
  const entries = []

  await mkdir(path.join(directory, 'files'), { recursive: true })
  for (const [file] of normalized) {
    const relative = relativeDisplay(root, file)
    if (relative.startsWith('../') || path.isAbsolute(relative)) {
      throw new CliError(`拒绝修改项目外文件：${file}`)
    }
    const existed = await pathExists(file)
    entries.push({ path: relative, existed })
    if (existed) {
      const destination = path.join(directory, 'files', ...relative.split('/'))
      await mkdir(path.dirname(destination), { recursive: true })
      await cp(file, destination)
    }
  }

  const metadata = { id, operation, createdAt: now.toISOString(), entries }
  await writeFile(path.join(directory, 'backup.json'), `${JSON.stringify(metadata, null, 2)}\n`, 'utf8')

  try {
    for (const [file, content] of normalized) await atomicWrite(file, content)
  } catch (error) {
    await restoreEntries(root, directory, entries)
    throw new CliError(`写入失败，已恢复原文件：${error.message}`, 1)
  }
  return metadata
}

async function restoreEntries(root, directory, entries) {
  for (const entry of [...entries].reverse()) {
    const target = path.join(root, ...entry.path.split('/'))
    if (entry.existed) {
      const source = path.join(directory, 'files', ...entry.path.split('/'))
      await mkdir(path.dirname(target), { recursive: true })
      await cp(source, target)
    } else {
      await rm(target, { force: true })
      await removeEmptyParents(path.dirname(target), root)
    }
  }
}

async function removeEmptyParents(directory, root) {
  let current = directory
  while (current !== root && current.startsWith(`${root}${path.sep}`)) {
    try {
      await rmdir(current)
    } catch {
      break
    }
    current = path.dirname(current)
  }
}

export async function rollbackBackup(root, id, dryRun = false) {
  if (!/^[A-Za-z0-9_-]+$/.test(id)) throw new CliError('备份编号格式不正确')
  const directory = path.join(root, '.nz-cli', 'backups', id)
  let metadata
  try {
    metadata = JSON.parse(await readFile(path.join(directory, 'backup.json'), 'utf8'))
  } catch {
    throw new CliError(`找不到备份：${id}`)
  }
  if (!Array.isArray(metadata.entries)) throw new CliError(`备份元数据损坏：${id}`)
  if (!dryRun) {
    await restoreEntries(root, directory, metadata.entries)
    metadata.rolledBackAt = new Date().toISOString()
    await writeFile(path.join(directory, 'backup.json'), `${JSON.stringify(metadata, null, 2)}\n`, 'utf8')
  }
  return metadata
}
