import { CliError } from './errors.mjs'

const BOOLEAN_OPTIONS = new Set([
  'backend-only',
  'compose',
  'dry-run',
  'force',
  'frontend-only',
  'help',
  'no-prepare',
  'yes',
])

export function parseOptions(argv) {
  const positionals = []
  const options = new Map()

  for (let index = 0; index < argv.length; index += 1) {
    const argument = argv[index]
    if (argument === '--') {
      positionals.push(...argv.slice(index + 1))
      break
    }
    if (!argument.startsWith('--')) {
      positionals.push(argument)
      continue
    }

    const separator = argument.indexOf('=')
    const key = argument.slice(2, separator === -1 ? undefined : separator)
    if (!key) throw new CliError(`无效参数：${argument}`)
    if (options.has(key)) throw new CliError(`参数重复：--${key}`)

    if (separator !== -1) {
      options.set(key, argument.slice(separator + 1))
    } else if (BOOLEAN_OPTIONS.has(key)) {
      options.set(key, true)
    } else {
      const value = argv[index + 1]
      if (value === undefined || value.startsWith('--')) {
        throw new CliError(`参数 --${key} 缺少值`)
      }
      options.set(key, value)
      index += 1
    }
  }

  return { positionals, options }
}

export function assertKnownOptions(options, allowed) {
  for (const key of options.keys()) {
    if (!allowed.has(key)) throw new CliError(`不支持参数：--${key}`)
  }
}

export function option(options, key, fallback) {
  return options.has(key) ? options.get(key) : fallback
}

export function requiredOption(options, key) {
  const value = option(options, key)
  if (typeof value !== 'string' || value.trim() === '') {
    throw new CliError(`缺少必填参数：--${key}`)
  }
  return value.trim()
}

export function integerOption(options, key, fallback) {
  const value = option(options, key, fallback)
  const number = Number(value)
  if (!Number.isSafeInteger(number) || number < 0) {
    throw new CliError(`参数 --${key} 必须是非负整数`)
  }
  return number
}

export function requireConfirmation(options, action) {
  if (option(options, 'dry-run', false)) return
  if (!option(options, 'yes', false)) {
    throw new CliError(`${action}会修改文件；先使用 --dry-run 检查，再添加 --yes 执行`)
  }
}
