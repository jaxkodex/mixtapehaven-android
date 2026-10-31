import test from 'node:test';
import assert from 'node:assert/strict';
import { getNextVersionCode, nextVersionCode, sourceVersionCode } from './play-version.mjs';

test('finds the literal source version and rejects ambiguous declarations', () => {
  assert.equal(sourceVersionCode('versionCode = 2\nversionCode = code\n'), 2);
  assert.throws(() => sourceVersionCode('versionCode = 2\nversionCode = 3\n'));
  assert.throws(() => sourceVersionCode('versionCode = code\n'));
});

test('chooses a code above the source and every Play artifact and track', () => {
  assert.equal(nextVersionCode(2, { bundles: [{ versionCode: 9 }] },
    { apks: [{ versionCode: 15 }] },
    { tracks: [{ releases: [{ versionCodes: ['20', '10'] }] }] }), 21);
  assert.equal(nextVersionCode(25, {}, {}, {}), 26);
  assert.equal(nextVersionCode(2, {}, {}, {}, 100), 101);
  assert.throws(() => nextVersionCode(2, {}, {}, {}, -1));
  assert.throws(() => nextVersionCode(2, { bundles: [{ versionCode: 'invalid' }] }, {}, {}));
  assert.throws(() => nextVersionCode(2_100_000_000, {}, {}, {}));
});

test('reads all Play artifact types and deletes its temporary edit', async () => {
  const calls = [];
  const request = async (url, options) => {
    calls.push([url, options.method]);
    let result = {};
    if (options.method === 'POST') result = { id: 'edit 1' };
    if (url.endsWith('/bundles')) result = { bundles: [{ versionCode: 4 }] };
    if (url.endsWith('/apks')) result = { apks: [{ versionCode: 8 }] };
    if (url.endsWith('/tracks')) result = { tracks: [{ releases: [{ versionCodes: ['12'] }] }] };
    return { ok: true, status: options.method === 'DELETE' ? 204 : 200, json: async () => result };
  };
  assert.equal(await getNextVersionCode('token', 2, request), 13);
  assert.deepEqual(calls.map(([url, method]) => [url.split('/').at(-1), method]), [
    ['edits', 'POST'], ['bundles', 'GET'], ['apks', 'GET'], ['tracks', 'GET'], ['edit%201', 'DELETE'],
  ]);
});

test('deletes the temporary edit even when a Play API read fails', async () => {
  const methods = [];
  const request = async (url, options) => {
    methods.push(options.method);
    if (options.method === 'POST') return { ok: true, status: 200, json: async () => ({ id: '1' }) };
    if (options.method === 'DELETE') return { ok: true, status: 204 };
    return { ok: false, status: 403, text: async () => 'Forbidden' };
  };
  await assert.rejects(getNextVersionCode('token', 2, request), /403/);
  assert.deepEqual(methods, ['POST', 'GET', 'DELETE']);
});
