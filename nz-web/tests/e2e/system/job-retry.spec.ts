import { execFileSync } from 'node:child_process'
import { randomUUID } from 'node:crypto'

import { expect, test } from '@nz/test/e2e'

import type { Locator } from '@playwright/test'


interface ApiResult<T> {
  code: number
  msg?: string
  data: T
}

interface JobReceipt {
  jobId: number
  revision: number
  scheduleState: string
}

interface JobRecord {
  id: number
  jobName: string
  jobGroup: string
  invokeTarget: string
  cronExpression: string
  status: number
  concurrent?: number
  misfirePolicy?: number
  remark?: string
  revision: number
  scheduleState: string
}

interface JobExecution {
  id: number
  jobId: number
  executionKey: string
  state: 'PENDING' | 'RUNNING' | 'SUCCEEDED' | 'FAILED' | 'SKIPPED'
  attempt: number
  retryable: boolean
  recoverable: boolean
  recovered: boolean
  lastError?: string
}

interface ExecutionReceipt {
  executionId: number
  executionKey: string
  state: JobExecution['state']
}

interface JobLog {
  id: number
  jobId: number
  jobName: string
  executionId?: number
  executionKey?: string
  status: number
  attempt?: number
}

interface Page<T> {
  records: T[]
  total: number
}

interface SqlClient {
  query(sql: string): string
  execute(sql: string): void
}

interface PostgresConfig {
  command: string
  databaseUrl: string
  env: Record<string, string | undefined>
  searchPath: string
  password: string
}

function postgresConfig(): PostgresConfig {
  const rawUrl = process.env.E2E_PG_URL ?? process.env.NZ_DATASOURCE_URL
  const username = process.env.PGUSER
  const password = process.env.PGPASSWORD
  if (!rawUrl || !username || !password) {
    throw new Error('E2E_JOB_FAULTS_ENABLED=true requires E2E_PG_URL or NZ_DATASOURCE_URL, PGUSER, and PGPASSWORD')
  }

  const parsed = new URL(rawUrl.replace(/^jdbc:/, ''))
  if (parsed.protocol !== 'postgres:' && parsed.protocol !== 'postgresql:') {
    throw new Error('The E2E PostgreSQL URL must use postgres:// or postgresql://')
  }
  const configuredSchemas = parsed.searchParams.get('currentSchema')
  parsed.searchParams.delete('currentSchema')
  parsed.searchParams.delete('user')
  parsed.searchParams.delete('password')
  parsed.username = ''
  parsed.password = ''

  const searchPath = configuredSchemas
    ? `SET search_path TO ${configuredSchemas.split(',').map(quoteIdentifier).join(', ')};\n`
    : ''
  return {
    command: process.env.E2E_PSQL_BIN || 'psql',
    databaseUrl: parsed.toString(),
    env: { ...process.env, PGUSER: username, PGPASSWORD: password },
    searchPath,
    password,
  }
}

function quoteIdentifier(identifier: string) {
  if (!/^[a-zA-Z_][a-zA-Z0-9_$]*$/.test(identifier)) {
    throw new Error('The PostgreSQL currentSchema contains an invalid identifier')
  }
  return `"${identifier.replaceAll('"', '""')}"`
}

function createSqlClient(config: PostgresConfig): SqlClient {
  function run(sql: string) {
    try {
      return execFileSync(config.command, [
        '--no-psqlrc',
        '--no-password',
        '--quiet',
        '--tuples-only',
        '--no-align',
        '--set=ON_ERROR_STOP=1',
        '--dbname',
        config.databaseUrl,
      ], {
        input: `${config.searchPath}${sql}\n`,
        encoding: 'utf8',
        env: config.env,
        timeout: 15_000,
        maxBuffer: 1024 * 1024,
        windowsHide: true,
      }).trim()
    } catch (error) {
      const details = error instanceof Error ? error.message : String(error)
      throw new Error(`psql failed: ${details.replaceAll(config.password, '[redacted]')}`, { cause: error })
    }
  }

  return {
    query: (sql) => run(sql).split(/\r?\n/).filter(Boolean).at(-1) ?? '',
    execute: (sql) => { run(sql) },
  }
}

function sqlLiteral(value: string) {
  return `'${value.replaceAll("'", "''")}'`
}

function createFaultSqlNames(suffix: string) {
  return {
    functionName: `e2e_job_retry_fn_${suffix}`,
    triggerName: `e2e_job_retry_tr_${suffix}`,
  }
}

function dropFaultTrigger(sql: SqlClient, names: ReturnType<typeof createFaultSqlNames>) {
  sql.execute(`
    DROP TRIGGER IF EXISTS ${names.triggerName} ON sys_job_log;
    DROP FUNCTION IF EXISTS ${names.functionName}();
  `)
}

function installFaultTrigger(
  sql: SqlClient,
  names: ReturnType<typeof createFaultSqlNames>,
  jobId: number,
  jobName: string,
) {
  dropFaultTrigger(sql, names)
  sql.execute(`
    CREATE FUNCTION ${names.functionName}() RETURNS trigger
    LANGUAGE plpgsql AS $e2e_fault$
    BEGIN
      IF NEW.job_id = ${jobId}
         AND NEW.job_name = ${sqlLiteral(jobName)}
         AND NEW.status = 0 THEN
        RAISE EXCEPTION 'E2E controlled success-log failure';
      END IF;
      RETURN NEW;
    END;
    $e2e_fault$;
    CREATE TRIGGER ${names.triggerName}
      BEFORE INSERT ON sys_job_log
      FOR EACH ROW EXECUTE FUNCTION ${names.functionName}();
  `)
}

test.describe('任务失败重试与旧配置保护', () => {
  test.setTimeout(180_000)
  test.use({ actionTimeout: 10_000 })
  test.skip(
    process.env.E2E_JOB_FAULTS_ENABLED !== 'true',
    'requires E2E_JOB_FAULTS_ENABLED=true and a dedicated PostgreSQL test database',
  )

  test('失败日志可保留并安全重试；定义升级后旧失败不可重放', async ({ authenticatedPage: page }) => {
    const database = postgresConfig()
    const sql = createSqlClient(database)
    const suffix = randomUUID().replaceAll('-', '').slice(0, 16)
    const jobName = `e2e_retry_${suffix}`
    const group = `E2E_${suffix}`
    const remark = `failure/retry fixture ${suffix}`
    const names = createFaultSqlNames(suffix)
    const token = await page.evaluate(() => localStorage.getItem('token') ?? '')
    expect(token).toBeTruthy()

    let jobId: number | undefined
    let bodyError: unknown
    let bodyFailed = false
    let cleanupError: unknown

    async function api<T>(method: string, path: string, data?: unknown) {
      const response = await page.request.fetch(path, {
        method,
        headers: { Authorization: token },
        data,
        timeout: 10_000,
      })
      const result = (await response.json()) as ApiResult<T>
      return { response, result }
    }

    async function requireApi<T>(method: string, path: string, data?: unknown) {
      const { response, result } = await api<T>(method, path, data)
      expect(response.ok(), `${method} ${path}: ${result.msg ?? response.status()}`).toBeTruthy()
      expect(result.code, result.msg).toBe(200)
      return result.data
    }

    async function readJob(id: number) {
      return requireApi<JobRecord>('GET', `/api/system/job/${id}`)
    }

    async function readExecution(id: number) {
      return requireApi<JobExecution>('GET', `/api/system/job/execution/${id}`)
    }

    async function waitForExecution(id: number, state: JobExecution['state'], retryable?: boolean) {
      let latest: JobExecution | undefined
      await expect.poll(async () => {
        latest = await readExecution(id)
        return `${latest.state}:${latest.attempt}:${latest.retryable}`
      }, { timeout: 45_000, intervals: [250, 500, 1000, 2000] }).toBe(
        retryable === undefined ? `${state}:2:false` : `${state}:1:${retryable}`,
      )
      if (!latest) throw new Error(`Execution ${id} did not become ${state}`)
      return latest
    }

    async function waitForSchedule(id: number, revision: number) {
      let latest: JobRecord | undefined
      await expect.poll(async () => {
        latest = await readJob(id)
        if (latest.revision !== revision) return 'REVISION_CHANGED'
        return latest.scheduleState
      }, { timeout: 30_000, intervals: [250, 500, 1000] }).toBe('SYNCED')
      if (!latest) throw new Error(`Job ${id} did not reach scheduleState=SYNCED`)
      return latest
    }

    async function clickRun(actions: Locator) {
      const responsePromise = page.waitForResponse((response) =>
        new URL(response.url()).pathname === `/api/system/job/run/${jobId}` &&
        response.request().method() === 'PUT',
      )
      await actions.getByRole('button', { name: 'Run', exact: true }).click()
      const response = await responsePromise
      expect(response.ok()).toBeTruthy()
      const result = (await response.json()) as ApiResult<ExecutionReceipt>
      expect(result.code, result.msg).toBe(200)
      expect(result.data.executionId).toBeGreaterThan(0)
      expect(result.data.executionKey).toBeTruthy()
      return result.data
    }

    async function cleanupFixture() {
      const failures: unknown[] = []
      try {
        dropFaultTrigger(sql, names)
      } catch (error) {
        failures.push(error)
      }
      try {
        if (!jobId) {
          const fixtureFilter = `job_name=${sqlLiteral(jobName)} AND job_group=${sqlLiteral(group)} ` +
            `AND remark=${sqlLiteral(remark)}`
          const matchingJobs = Number(sql.query(`SELECT COUNT(*) FROM sys_job WHERE ${fixtureFilter};`))
          if (matchingJobs > 1) throw new Error('Fixture identity matched multiple job definitions')
          if (matchingJobs === 1) jobId = Number(sql.query(`SELECT id FROM sys_job WHERE ${fixtureFilter};`))
        }
        if (jobId) {
          const current = await api<JobRecord>('GET', `/api/system/job/${jobId}`)
          const jobExists = current.response.ok() && current.result.code === 200
          if (jobExists && (current.result.data.id !== jobId || current.result.data.jobName !== jobName)) {
            throw new Error(`Refusing to clean a job outside this fixture: ${jobId}`)
          }
          const logs = await api<Page<JobLog>>(
            'GET', `/api/system/job/log/page?pageNum=1&pageSize=100&jobId=${jobId}`,
          )
          if (logs.response.ok() && logs.result.code === 200 && logs.result.data.records.length) {
            const fixtureLogs = logs.result.data.records.filter((log) =>
              log.jobId === jobId && log.jobName === jobName,
            )
            if (fixtureLogs.length !== logs.result.data.records.length) {
              throw new Error(`Refusing to clean logs outside this fixture: ${jobId}`)
            }
            await requireApi<void>('DELETE', '/api/system/job/log', fixtureLogs.map((log) => log.id))
          }
          if (jobExists) {
            await requireApi<JobReceipt>('DELETE', `/api/system/job/${jobId}`)
          }
        }
      } catch (error) {
        failures.push(error)
      }
      if (failures.length) throw new AggregateError(failures, 'Retry fixture cleanup failed')
    }

    try {
      const created = await requireApi<JobReceipt>('POST', '/api/system/job', {
        jobName,
        jobGroup: group,
        invokeTarget: 'demoTask.run',
        cronExpression: '0 0 0 1 1 ? 2099',
        status: 0,
        concurrent: 1,
        misfirePolicy: 3,
        remark,
      })
      jobId = created.jobId
      expect(jobId).toBeGreaterThan(0)
      await waitForSchedule(jobId, created.revision)
      expect(sql.query(`SELECT COUNT(*) FROM sys_job WHERE id=${jobId} AND job_name=${sqlLiteral(jobName)};`))
        .toBe('1')

      await page.getByRole('menuitem', { name: '定时任务', exact: true }).click()
      await page.getByRole('textbox', { name: 'Job name', exact: true }).fill(jobName)
      await page.getByRole('button', { name: 'Search', exact: true }).click()
      const actions = page.getByTestId(`job-row-${jobId}`)
      await expect(actions).toBeVisible()

      installFaultTrigger(sql, names, jobId, jobName)
      const firstReceipt = await clickRun(actions)
      const firstFailure = await waitForExecution(firstReceipt.executionId, 'FAILED', true)
      expect(firstFailure.lastError).toContain('E2E controlled success-log failure')
      expect(firstFailure).toMatchObject({
        id: firstReceipt.executionId,
        jobId,
        executionKey: firstReceipt.executionKey,
        state: 'FAILED',
        attempt: 1,
        recoverable: true,
        retryable: true,
      })
      expect(sql.query(`SELECT COUNT(*) FROM sys_job_log WHERE job_id=${jobId} ` +
        `AND job_name=${sqlLiteral(jobName)} AND execution_id=${firstReceipt.executionId} ` +
        'AND status=1 AND attempt=1;')).toBe('1')

      const recordsDialog = page.getByRole('dialog', { name: 'Job executions and logs' })
      await expect(recordsDialog).toBeVisible()
      const executionsPanel = recordsDialog.getByRole('tabpanel', { name: 'Executions', exact: true })
      const firstRow = executionsPanel.getByRole('row').filter({ hasText: firstReceipt.executionKey })
      await expect(firstRow.getByRole('button', { name: 'Retry', exact: true })).toBeVisible()

      dropFaultTrigger(sql, names)
      const retryResponsePromise = page.waitForResponse((response) =>
        new URL(response.url()).pathname === `/api/system/job/execution/${firstReceipt.executionId}/retry` &&
        response.request().method() === 'PUT',
      )
      await firstRow.getByRole('button', { name: 'Retry', exact: true }).click()
      const retryResponse = await retryResponsePromise
      expect(retryResponse.ok()).toBeTruthy()
      const retryResult = (await retryResponse.json()) as ApiResult<ExecutionReceipt>
      expect(retryResult.code, retryResult.msg).toBe(200)
      expect(retryResult.data).toMatchObject({
        executionId: firstReceipt.executionId,
        executionKey: firstReceipt.executionKey,
        state: 'PENDING',
      })

      const succeeded = await waitForExecution(firstReceipt.executionId, 'SUCCEEDED')
      expect(succeeded).toMatchObject({
        id: firstReceipt.executionId,
        jobId,
        executionKey: firstReceipt.executionKey,
        state: 'SUCCEEDED',
        attempt: 2,
        recovered: true,
        recoverable: true,
        retryable: false,
      })
      await expect(firstRow.getByText('Recovered', { exact: true })).toBeVisible()
      expect(sql.query(`SELECT COUNT(*) FROM sys_job_execution WHERE id=${firstReceipt.executionId} ` +
        `AND job_id=${jobId} AND execution_key=${sqlLiteral(firstReceipt.executionKey)} ` +
        "AND state='SUCCEEDED' AND attempt=2;" )).toBe('1')
      expect(sql.query(`SELECT COUNT(*) FROM sys_job_log WHERE job_id=${jobId} ` +
        `AND execution_id=${firstReceipt.executionId} AND execution_key=${sqlLiteral(firstReceipt.executionKey)} ` +
        'AND status=1 AND attempt=1;')).toBe('1')
      expect(sql.query(`SELECT COUNT(*) FROM sys_job_log WHERE job_id=${jobId} ` +
        `AND execution_id=${firstReceipt.executionId} AND execution_key=${sqlLiteral(firstReceipt.executionKey)} ` +
        'AND status=0 AND attempt=2;')).toBe('1')

      await expect.poll(async () => {
        await recordsDialog.getByRole('tab', { name: 'Logs' }).click()
        const logs = await requireApi<Page<JobLog>>(
          'GET', `/api/system/job/log/page?pageNum=1&pageSize=100&jobId=${jobId}`,
        )
        return logs.records.filter((log) => log.executionId === firstReceipt.executionId).length
      }, { timeout: 30_000, intervals: [250, 500, 1000] }).toBe(2)
      const firstLogs = recordsDialog.getByRole('tabpanel', { name: 'Logs', exact: true })
        .getByRole('row').filter({ hasText: firstReceipt.executionKey })
      await expect(firstLogs.filter({ hasText: 'Failed' })).toBeVisible()
      await expect(firstLogs.filter({ hasText: 'Succeeded' })).toBeVisible()
      await page.keyboard.press('Escape')
      await expect(recordsDialog).toBeHidden()

      installFaultTrigger(sql, names, jobId, jobName)
      const secondReceipt = await clickRun(actions)
      const staleFailure = await waitForExecution(secondReceipt.executionId, 'FAILED', true)
      expect(staleFailure.lastError).toContain('E2E controlled success-log failure')
      expect(staleFailure.executionKey).toBe(secondReceipt.executionKey)
      expect(sql.query(`SELECT COUNT(*) FROM sys_job_log WHERE job_id=${jobId} ` +
        `AND job_name=${sqlLiteral(jobName)} AND execution_id=${secondReceipt.executionId} ` +
        'AND status=1 AND attempt=1;')).toBe('1')

      const beforeUpdate = await readJob(jobId)
      const updated = await requireApi<JobReceipt>('PUT', '/api/system/job', {
        id: jobId,
        jobName: beforeUpdate.jobName,
        jobGroup: beforeUpdate.jobGroup,
        invokeTarget: beforeUpdate.invokeTarget,
        cronExpression: beforeUpdate.cronExpression,
        status: beforeUpdate.status,
        concurrent: beforeUpdate.concurrent ?? 1,
        misfirePolicy: beforeUpdate.misfirePolicy ?? 3,
        remark: `${beforeUpdate.remark ?? ''} revision ${suffix}`,
      })
      expect(updated.revision).toBeGreaterThan(beforeUpdate.revision)
      await waitForSchedule(jobId, updated.revision)
      const staleExecution = await readExecution(secondReceipt.executionId)
      expect(staleExecution).toMatchObject({ state: 'FAILED', attempt: 1, retryable: false })

      await page.keyboard.press('Escape')
      await expect(recordsDialog).toBeHidden()
      await actions.getByRole('button', { name: 'Executions & logs', exact: true }).click()
      await expect(recordsDialog).toBeVisible()
      await recordsDialog.getByRole('tab', { name: 'Executions', exact: true }).click()
      await recordsDialog.getByRole('button', { name: 'Refresh records', exact: true }).click()
      const staleRow = recordsDialog.getByRole('tabpanel', { name: 'Executions', exact: true })
        .getByRole('row').filter({ hasText: secondReceipt.executionKey })
      await expect(staleRow).toBeVisible()
      await expect(staleRow.getByRole('button', { name: 'Retry', exact: true })).toHaveCount(0)

      const rejected = await api<ExecutionReceipt>(
        'PUT', `/api/system/job/execution/${secondReceipt.executionId}/retry`,
      )
      expect(rejected.result.code, rejected.result.msg).not.toBe(200)
      expect(rejected.result.msg).toContain('当前配置')
      expect((await readExecution(secondReceipt.executionId)).attempt).toBe(1)
      expect(sql.query(`SELECT COUNT(*) FROM sys_job_execution WHERE id=${secondReceipt.executionId} ` +
        `AND execution_key=${sqlLiteral(secondReceipt.executionKey)} AND state='FAILED' AND attempt=1;`))
        .toBe('1')
    } catch (error) {
      bodyFailed = true
      bodyError = error
    } finally {
      try {
        await cleanupFixture()
      } catch (error) {
        cleanupError = error
      }
    }

    if (bodyFailed && cleanupError) {
      throw new AggregateError([bodyError, cleanupError], 'Retry E2E failed and cleanup also failed')
    }
    if (bodyFailed) throw bodyError
    if (cleanupError) throw cleanupError
  })
})
