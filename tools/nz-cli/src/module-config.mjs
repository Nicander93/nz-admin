import { CliError } from './errors.mjs'

export function setModuleEnabled(content, code, enabled) {
  const newline = content.includes('\r\n') ? '\r\n' : '\n'
  const lines = content.replace(/\r\n/g, '\n').split('\n')
  const modulesIndex = lines.findIndex((line) => line === '  modules:')
  if (modulesIndex === -1) throw new CliError('application.yml 缺少 nz.modules 配置段')

  let endIndex = lines.length
  for (let index = modulesIndex + 1; index < lines.length; index += 1) {
    if (/^  [A-Za-z0-9_-]+:/.test(lines[index])) {
      endIndex = index
      break
    }
  }

  const moduleLine = `    ${code}:`
  const moduleIndex = lines.findIndex((line, index) => index > modulesIndex && index < endIndex && line === moduleLine)
  if (moduleIndex === -1) {
    lines.splice(endIndex, 0, moduleLine, `      enabled: ${enabled}`)
  } else {
    let enabledIndex = -1
    for (let index = moduleIndex + 1; index < endIndex; index += 1) {
      if (/^    [A-Za-z0-9_-]+:/.test(lines[index])) break
      if (lines[index].startsWith('      enabled:')) {
        enabledIndex = index
        break
      }
    }
    if (enabledIndex === -1) lines.splice(moduleIndex + 1, 0, `      enabled: ${enabled}`)
    else lines[enabledIndex] = `      enabled: ${enabled}`
  }
  return lines.join(newline)
}
