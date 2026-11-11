import '@testing-library/jest-dom/vitest';

// jsdom doesn't expose localStorage — Zustand persist middleware crashes without it
// adding an in-memory stub so stores using persist (useAuthStore, useThemeStore) work in tests
class InMemStorage implements Storage {
  private data: Record<string, string> = {};

  get length() { return Object.keys(this.data).length; }
  key(i: number) { return Object.keys(this.data)[i] ?? null; }
  getItem(k: string) { return this.data[k] ?? null; }
  setItem(k: string, v: string) { this.data[k] = String(v); }
  removeItem(k: string) { delete this.data[k]; }
  clear() { this.data = {}; }
}

const fakeLocal = new InMemStorage();
const fakeSession = new InMemStorage();

Object.defineProperty(globalThis, 'localStorage', { value: fakeLocal, writable: true });
Object.defineProperty(globalThis, 'sessionStorage', { value: fakeSession, writable: true });

beforeEach(() => {
  fakeLocal.clear();
  fakeSession.clear();
});
