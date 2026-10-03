import { expect, it, vi } from 'vitest'

import { withIdempotency } from '@/utils/idempotency'

it('retains the request key after ambiguous network failure and resets after success', async () => {
  const submit = vi
    .fn()
    .mockRejectedValueOnce(new Error('network'))
    .mockResolvedValue('ok')
  const run = withIdempotency(submit)
  await expect(run({ name: 'first' })).rejects.toThrow('network')
  await run({ name: 'first' })
  expect(submit.mock.calls[0]![1]).toEqual(submit.mock.calls[1]![1])
  await run({ name: 'first' })
  expect(submit.mock.calls[2]![1]).not.toEqual(submit.mock.calls[1]![1])
})
it('uses a new request key when the payload changes', async () => {
  const submit = vi.fn().mockRejectedValue(new Error('network'))
  const run = withIdempotency(submit)
  await expect(run({ name: 'first' })).rejects.toThrow()
  await expect(run({ name: 'second' })).rejects.toThrow()
  expect(submit.mock.calls[0]![1]).not.toEqual(submit.mock.calls[1]![1])
})
