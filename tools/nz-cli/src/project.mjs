import { access, readFile } from 'node:fs/promises'
import path from 'node:path'
import { CliError } from './errors.mjs'

async function exists(file) {
  try {
    await access(file)
    return true
  } catch {
    return false
  }
}

export async function findProjectRoot(start = process.cwd()) {
  let current = path.resolve(start)
  while (true) {
    if (
      await exists(path.join(current, 'nz-server', 'pom.xml')) &&
      await exists(path.join(current, 'nz-web', 'package.json'))
    ) {
      return current
    }
    const parent = path.dirname(current)
    if (parent === current) break
    current = parent
  }
  throw new CliError('当前目录不在 nz-admin 项目内；可通过 --root 指定项目根目录')
}

export async function readUtf8(file) {
  const content = await readFile(file, 'utf8')
  if (content.charCodeAt(0) === 0xfeff) return content.slice(1)
  return content
}

export function relativeDisplay(root, file) {
  return path.relative(root, file).split(path.sep).join('/')
}
