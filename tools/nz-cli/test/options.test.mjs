import assert from 'node:assert/strict'
import { describe, it } from 'node:test'
import { CliError } from '../src/errors.mjs'
import { integerOption, parseOptions, requireConfirmation } from '../src/options.mjs'

describe('CLI options', () => {
  it('parses positionals, values and boolean flags', () => {
    const result = parseOptions(['module', 'add', 'audit', '--title', '审计', '--dry-run'])
    assert.deepEqual(result.positionals, ['module', 'add', 'audit'])
    assert.equal(result.options.get('title'), '审计')
    assert.equal(result.options.get('dry-run'), true)
  })

  it('rejects mutation without dry-run or confirmation', () => {
    assert.throws(() => requireConfirmation(new Map(), '创建模块'), CliError)
    assert.doesNotThrow(() => requireConfirmation(new Map([['dry-run', true]]), '创建模块'))
    assert.doesNotThrow(() => requireConfirmation(new Map([['yes', true]]), '创建模块'))
  })

  it('validates integer options', () => {
    assert.equal(integerOption(new Map([['menu-id', '42']]), 'menu-id'), 42)
    assert.throws(() => integerOption(new Map([['menu-id', '-1']]), 'menu-id'), CliError)
  })
})
