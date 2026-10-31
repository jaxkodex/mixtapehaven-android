import { readFile } from 'node:fs/promises';
import { pathToFileURL } from 'node:url';

const MAX_PLAY_VERSION_CODE = 2_100_000_000;
const PACKAGE_NAME = 'pe.net.libre.mixtapehaven';

export function sourceVersionCode(buildFile) {
  const matches = [...buildFile.matchAll(/^[ \t]*versionCode[ \t]*=[ \t]*(\d+)[ \t]*\r?$/gm)];
  if (matches.length !== 1) {
    throw new Error('Expected exactly one literal versionCode in app/build.gradle.kts');
  }
  return Number(matches[0][1]);
}

export function nextVersionCode(sourceCode, bundles, apks, tracks, versionFloor = 0) {
  if (!Number.isSafeInteger(versionFloor) || versionFloor < 0) {
    throw new Error('PLAY_VERSION_CODE_FLOOR must be a non-negative integer');
  }
  const codes = [sourceCode, ...(versionFloor ? [versionFloor] : [])];
  for (const artifact of [...(bundles.bundles ?? []), ...(apks.apks ?? [])]) {
    codes.push(artifact.versionCode);
  }
  for (const track of tracks.tracks ?? []) {
    for (const release of track.releases ?? []) {
      codes.push(...(release.versionCodes ?? []));
    }
  }
  const parsed = codes.map((code) => Number(code));
  if (parsed.some((code) => !Number.isSafeInteger(code) || code < 1)) {
    throw new Error('Play returned an invalid version code');
  }
  const next = Math.max(...parsed) + 1;
  if (next > MAX_PLAY_VERSION_CODE) {
    throw new Error('No Google Play version codes remain');
  }
  return next;
}

export async function getNextVersionCode(token, sourceCode, request = fetch, versionFloor = 0) {
  const base = `https://androidpublisher.googleapis.com/androidpublisher/v3/applications/${PACKAGE_NAME}/edits`;
  async function call(url, method = 'GET') {
    const response = await request(url, {
      method,
      headers: { Authorization: `Bearer ${token}`, 'Content-Type': 'application/json' },
      ...(method === 'POST' ? { body: '{}' } : {}),
    });
    if (!response.ok) {
      throw new Error(`Google Play API ${method} ${url} failed (${response.status}): ${await response.text()}`);
    }
    return response.status === 204 ? {} : response.json();
  }

  const edit = await call(base, 'POST');
  if (!edit.id) throw new Error('Google Play did not return an edit ID');
  const editUrl = `${base}/${encodeURIComponent(edit.id)}`;
  try {
    const bundles = await call(`${editUrl}/bundles`);
    const apks = await call(`${editUrl}/apks`);
    const tracks = await call(`${editUrl}/tracks`);
    return nextVersionCode(sourceCode, bundles, apks, tracks, versionFloor);
  } finally {
    await call(editUrl, 'DELETE');
  }
}

if (process.argv[1] && import.meta.url === pathToFileURL(process.argv[1]).href) {
  try {
    if (!process.env.PLAY_ACCESS_TOKEN) throw new Error('PLAY_ACCESS_TOKEN is required');
    const source = await readFile('app/build.gradle.kts', 'utf8');
    const floor = process.env.PLAY_VERSION_CODE_FLOOR ?? '0';
    if (!/^\d+$/.test(floor)) throw new Error('PLAY_VERSION_CODE_FLOOR must be a non-negative integer');
    const code = await getNextVersionCode(process.env.PLAY_ACCESS_TOKEN, sourceVersionCode(source), fetch, Number(floor));
    console.log(`version_code=${code}`);
  } catch (error) {
    console.error(error.message);
    process.exitCode = 1;
  }
}
