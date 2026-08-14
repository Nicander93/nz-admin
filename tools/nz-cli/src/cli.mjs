import path from 'node:path'
import { rollbackBackup } from './backup.mjs'
import { downloadCode } from './codegen.mjs'
import { doctor } from './doctor.mjs'
import { checkDelivery, runDeliveryCompose, smokeDelivery } from './delivery.mjs'
import { CliError } from './errors.mjs'
import { checkMigrations } from './migrations.mjs'
import { addModule } from './module-add.mjs'
import { toggleModule } from './module-toggle.mjs'
import { assertKnownOptions, integerOption, option, parseOptions, requiredOption, requireConfirmation } from './options.mjs'
import { buildProject, devProject, verifyProject } from './processes.mjs'
import { findProjectRoot, relativeDisplay } from './project.mjs'
import { renameProject } from './rename.mjs'

const GLOBAL_OPTIONS = new Set(['help', 'root'])
const HELP = `nz-admin 项目命令

用法：
  ./nz doctor
  ./nz dev [--no-prepare]
  ./nz build [--backend-only | --frontend-only]
  ./nz verify [--backend-only | --frontend-only]
  ./nz migration check
  ./nz delivery check [--env <deploy/.env>] [--compose]
  ./nz delivery smoke [--url <http://host>]
  ./nz delivery up [--env <deploy/.env>] [--profile <redis,storage>] --yes
  ./nz delivery ps [--env <deploy/.env>] [--profile <redis,storage>]
  ./nz delivery down [--env <deploy/.env>] [--profile <redis,storage>] --yes
  ./nz module add <code> --title <名称> [--description <说明>] [--parent-menu-id <id>] [--menu-id <id>] --dry-run
  ./nz module add <code> --title <名称> [参数...] --yes
  ./nz module enable <code> --dry-run | --yes
  ./nz module disable <code> --dry-run | --yes
  ./nz codegen --request <json> [--server <url>] [--token <token>] [--output <zip>] --dry-run | --yes
  ./nz rename --from <原文本> --to <新文本> --dry-run | --yes
  ./nz rollback <backup-id> --dry-run | --yes

说明：
  修改型命令必须选择 --dry-run 或 --yes。
  实际修改会写入 .nz-cli/backups，可用 rollback 恢复。
  --root 可在任意命令中指定项目根目录。
`

function allowed(...values) {
  return new Set([...GLOBAL_OPTIONS, ...values])
}

function assertOneSide(options) {
  if (option(options, 'backend-only', false) && option(options, 'frontend-only', false)) {
    throw new CliError('--backend-only 和 --frontend-only 不能同时使用')
  }
}

function printChangePlan(root, changes, write) {
  const files = [...changes.keys()].map((file) => relativeDisplay(root, file)).sort()
  write(`将修改 ${files.length} 个文件：`)
  for (const file of files) write(`  ${file}`)
}

export async function runCli(argv, context = {}) {
  const write = context.write || console.log
  const error = context.error || console.error
  const { positionals, options } = parseOptions(argv)
  const command = positionals[0]

  if (!command || option(options, 'help', false) || command === 'help') {
    write(HELP)
    return 0
  }

  const start = path.resolve(String(option(options, 'root', context.cwd || process.cwd())))
  const root = await findProjectRoot(start)

  if (command === 'doctor') {
    assertKnownOptions(options, allowed())
    const result = await doctor(root, context.probe)
    for (const check of result.checks) write(`${check.ok ? 'OK' : 'FAIL'}  ${check.name}：${check.detail}`)
    return result.ok ? 0 : 1
  }

  if (command === 'migration' && positionals[1] === 'check') {
    assertKnownOptions(options, allowed())
    const result = await checkMigrations(root)
    write(`迁移检查通过：V1-V${result.nextVersion - 1}，${result.upgrades.length} 个手工升级脚本`)
    return 0
  }

  if (command === 'delivery' && positionals[1] === 'check') {
    assertKnownOptions(options, allowed('compose', 'env'))
    const envFile = option(options, 'env')
    const result = await checkDelivery(root, { envFile })
    if (option(options, 'compose', false)) {
      if (!envFile) throw new CliError('--compose 必须同时指定 --env')
      await runDeliveryCompose(root, {
        action: 'config',
        envFile,
        profiles: result.profiles.join(','),
      }, context.spawnSync)
    }
    write(`交付检查通过：${result.files} 个文件，${result.services} 个服务`)
    return 0
  }

  if (command === 'delivery' && positionals[1] === 'smoke') {
    assertKnownOptions(options, allowed('url'))
    const results = await smokeDelivery(option(options, 'url', 'http://127.0.0.1'), context.fetch)
    for (const result of results) write(`OK  ${result.name}：${result.url}`)
    return 0
  }
  if (command === 'delivery' && ['up', 'ps', 'down'].includes(positionals[1])) {
    const action = positionals[1]
    const changesRuntime = action !== 'ps'
    assertKnownOptions(options, allowed('env', 'profile', ...(changesRuntime ? ['yes'] : [])))
    if (changesRuntime && !option(options, 'yes', false)) {
      throw new CliError(`delivery ${action} 会修改容器运行状态；确认后添加 --yes`)
    }
    const result = await runDeliveryCompose(root, {
      action,
      envFile: option(options, 'env', 'deploy/.env'),
      profiles: option(options, 'profile', ''),
    }, context.spawnSync)
    write(`交付环境 ${action} 完成；profiles：${result.profiles.join(', ') || '基础环境'}`)
    return 0
  }

  if (command === 'build' || command === 'verify') {
    assertKnownOptions(options, allowed('backend-only', 'frontend-only'))
    assertOneSide(options)
    const processOptions = {
      backendOnly: option(options, 'backend-only', false),
      frontendOnly: option(options, 'frontend-only', false),
    }
    if (command === 'build') buildProject(root, processOptions, context.spawnSync)
    else verifyProject(root, processOptions, context.spawnSync)
    write(`${command === 'build' ? '构建' : '验证'}通过`)
    return 0
  }

  if (command === 'dev') {
    assertKnownOptions(options, allowed('no-prepare'))
    devProject(root, { noPrepare: option(options, 'no-prepare', false) }, context)
    write('后端与前端开发进程已启动，按 Ctrl+C 一并停止')
    return 0
  }

  if (command === 'module' && positionals[1] === 'add') {
    assertKnownOptions(options, allowed('description', 'dry-run', 'menu-id', 'parent-menu-id', 'title', 'yes'))
    const code = positionals[2]
    if (!code || positionals.length !== 3) throw new CliError('用法：./nz module add <code> --title <名称>')
    requireConfirmation(options, '创建模块')
    const result = await addModule(root, {
      code,
      title: requiredOption(options, 'title'),
      description: option(options, 'description'),
      parentMenuId: integerOption(options, 'parent-menu-id', 0),
      menuId: options.has('menu-id') ? integerOption(options, 'menu-id') : undefined,
    }, option(options, 'dry-run', false))
    printChangePlan(root, result.changes, write)
    write(`Flyway：${result.migrationName}；菜单 ID：${result.menuId}`)
    if (result.backup) write(`备份：${result.backup.id}`)
    return 0
  }

  if (command === 'module' && (positionals[1] === 'enable' || positionals[1] === 'disable')) {
    assertKnownOptions(options, allowed('dry-run', 'yes'))
    const code = positionals[2]
    if (!code || positionals.length !== 3) throw new CliError(`用法：./nz module ${positionals[1]} <code>`)
    requireConfirmation(options, `${positionals[1] === 'enable' ? '启用' : '禁用'}模块`)
    const enabled = positionals[1] === 'enable'
    const result = await toggleModule(root, code, enabled, option(options, 'dry-run', false))
    write(`模块 ${code} 将设置为 ${enabled ? '启用' : '禁用'}，重启后生效`)
    if (result.backup) write(`备份：${result.backup.id}`)
    return 0
  }

  if (command === 'codegen') {
    assertKnownOptions(options, allowed('dry-run', 'output', 'request', 'server', 'token', 'yes'))
    requireConfirmation(options, '下载生成代码')
    const result = await downloadCode(root, {
      requestFile: requiredOption(options, 'request'),
      server: option(options, 'server', 'http://localhost:8080'),
      token: option(options, 'token', process.env.NZ_TOKEN),
      output: option(options, 'output'),
    }, option(options, 'dry-run', false), context.fetch)
    write(`请求表：${result.request.schemaName}.${result.request.tableName}`)
    write(`输出：${relativeDisplay(root, result.output)}${result.bytes ? `（${result.bytes} 字节）` : ''}`)
    if (result.backup) write(`备份：${result.backup.id}`)
    return 0
  }

  if (command === 'rename') {
    assertKnownOptions(options, allowed('dry-run', 'from', 'to', 'yes'))
    requireConfirmation(options, '替换项目标识')
    const result = await renameProject(root, requiredOption(options, 'from'), requiredOption(options, 'to'), option(options, 'dry-run', false))
    write(`命中 ${result.matches.length} 个文件，共 ${result.matches.reduce((sum, item) => sum + item.count, 0)} 处`)
    for (const match of result.matches.slice(0, 30)) write(`  ${match.path}（${match.count}）`)
    if (result.matches.length > 30) write(`  其余 ${result.matches.length - 30} 个文件未展开`)
    if (result.backup) write(`备份：${result.backup.id}`)
    return 0
  }

  if (command === 'rollback') {
    assertKnownOptions(options, allowed('dry-run', 'yes'))
    const id = positionals[1]
    if (!id || positionals.length !== 2) throw new CliError('用法：./nz rollback <backup-id>')
    requireConfirmation(options, '回滚备份')
    const result = await rollbackBackup(root, id, option(options, 'dry-run', false))
    write(`${option(options, 'dry-run', false) ? '将恢复' : '已恢复'} ${result.entries.length} 个文件：${id}`)
    return 0
  }

  error(`未知命令：${positionals.join(' ')}`)
  write(HELP)
  return 2
}
