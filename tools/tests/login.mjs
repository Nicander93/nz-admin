import assert from "node:assert/strict";
import { setTimeout as delay } from "node:timers/promises";

/** 测试客户端遵守生产频控，只在服务端给出明确等待时间时重试一次。 */
export async function loginByHttp(baseURL, credentials) {
  for (let attempt = 0; attempt < 2; attempt++) {
    const response = await fetch(`${baseURL}/api/auth/login`, {
      method: "POST",
      signal: AbortSignal.timeout(15000),
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify(credentials),
    });
    const body = await response.json();
    const waiting = Number(response.headers.get("retry-after"));
    if (
      attempt === 0 &&
      body.code === 429 &&
      Number.isFinite(waiting) &&
      waiting > 0 &&
      waiting <= 120
    ) {
      await delay(waiting * 1000 + 100);
      continue;
    }
    assert.equal(body.code, 200, `POST /api/auth/login: ${body.msg}`);
    assert.equal(typeof body.data, "string", "Login must return a token");
    return body.data;
  }
}
