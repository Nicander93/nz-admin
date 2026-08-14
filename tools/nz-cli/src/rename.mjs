import { readdir, readFile } from 'node:fs/promises'
import path from 'node:path'
import { applyChanges } from './backup.mjs'
import { invariant } from './errors.mjs'
import { relativeDisplay } from './project.mjs'

const EXCLUDED_DIRECTORIES = new Set([
  '.git',
  '.nz-cli',
  '.serena',
  'coverage',
  'dist',
  'node_modules',
  'ruoyi-vue-pro',
  'target',
])

const TEXT_EXTENSIONS = new Set([
  '.cmd',
  '.css',
  '.env',
  '.gitignore',
  '.html',
  '.java',
  '.json',
  '.md',
  '.mjs',
  '.properties',
  '.sh',
  '.sql',
  '.ts',
  '.tsx',
  '.txt',
  '.vue',
  '.xml',
  '.yaml',
  '.yml',
])

function isTextFile(name) {
  const lower = name.toLowerCase()
  return lower === 'readme' || lower === 'mvnw' || TEXT_EXTENSIONS.has(path.extname(lower))
}

async function collectTextFiles(directory, result = []) {
  for (const entry of await readdir(directory, { withFileTypes: true })) {
    if (entry.isSymbolicLink()) continue
    const file = path.join(directory, entry.name)
    if (entry.isDirectory()) {
      if (!EXCLUDED_DIRECTORIES.has(entry.name)) await collectTextFiles(file, result)
    } else if (entry.isFile() && isTextFile(entry.name)) {
      result.push(file)
    }
  }
  return result
}

export async function planRename(root, from, to) {
  invariant(typeof from === 'string' && from.length > 0, '--from 不能为空')
  invariant(typeof to === 'string' && to.length > 0, '--to 不能为空')
  invariant(from !== to, '--from 和 --to 不能相同')
  invariant(!/[\r\n\0]/.test(from + to), '重命名文本不能包含换行或空字符')
  invariant(from.length <= 120 && to.length <= 120, '重命名文本不能超过 120 个字符')

  const changes = new Map()
  const matches = []
  for (const file of await collectTextFiles(root)) {
    const buffer = await readFile(file)
    if (buffer.length > 2 * 1024 * 1024 || buffer.includes(0)) continue
    const content = buffer.toString('utf8')
    if (!content.includes(from)) continue
    const count = content.split(from).length - 1
    changes.set(file, content.replaceAll(from, to))
    matches.push({ path: relativeDisplay(root, file), count })
  }
  invariant(changes.size > 0, `项目中没有找到文本：${from}`)
  return { from, to, changes, matches }
}

export async function renameProject(root, from, to, dryRun = false) {
  const plan = await planRename(root, from, to)
  if (dryRun) return { ...plan, backup: undefined }
  const backup = await applyChanges(root, 'rename', plan.changes)
  return { ...plan, backup }
}
