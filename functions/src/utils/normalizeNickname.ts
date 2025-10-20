export function normalizeNickname(raw: string) {
  const trimmed = raw.trim();
  const nfkc = trimmed.normalize('NFKC');
  const lower = nfkc.toLowerCase();
  const valid = /^[가-힣a-z0-9_]{2,16}$/;
  if (!valid.test(lower)) return { ok:false as const, reason:'INVALID_FORMAT' };
  const blocked = new Set(['admin','operator','관리자']);
  if (blocked.has(lower)) return { ok:false as const, reason:'RESERVED' };
  return { ok:true as const, key:lower, pretty:trimmed };
}
