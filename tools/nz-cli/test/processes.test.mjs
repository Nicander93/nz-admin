import assert from 'node:assert/strict'
import { describe, it } from 'node:test'
import { buildProject, verifyProject } from '../src/processes.mjs'

function recorder(calls) {
  return (command, args, options) => {
    calls.push({ command, args, options })
    return { status: 0 }
  }
}

describe('build and verify commands', () => {
  it('builds backend and frontend by default', () => {
    const calls = []
    buildProject('/project', {}, recorder(calls))
    assert.equal(calls.length, 2)
    assert.deepEqual(calls[0].args, ['-pl', 'nz-app', '-am', 'package', '-DskipTests'])
    assert.deepEqual(calls[1].args, ['build'])
  })

  it('runs backend tests and both frontend gates during verification', () => {
    const calls = []
    verifyProject('/project', {}, recorder(calls))
    assert.equal(calls.length, 4)
    assert.deepEqual(calls.map((call) => call.args.at(-1)), ['test', 'test', 'build', '--test'])
    assert.match(calls[3].options.cwd, /tools[\\/]nz-cli$/)
  })

  it('honors backend-only mode', () => {
    const calls = []
    verifyProject('/project', { backendOnly: true }, recorder(calls))
    assert.equal(calls.length, 1)
    assert.equal(calls[0].args.at(-1), 'test')
  })
})
