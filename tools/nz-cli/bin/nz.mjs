#!/usr/bin/env node
import { runCli } from '../src/cli.mjs'
import { CliError } from '../src/errors.mjs'

try {
  const exitCode = await runCli(process.argv.slice(2))
  process.exitCode = exitCode
} catch (error) {
  if (error instanceof CliError) {
    console.error(`错误：${error.message}`)
    process.exitCode = error.exitCode
  } else {
    console.error(error)
    process.exitCode = 1
  }
}
