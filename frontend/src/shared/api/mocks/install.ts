/** F-N08, SECURITY-09: enables browser mocks only in an explicit development build. */
export const installMockTransport = async (): Promise<void> => {
  if (!import.meta.env.DEV || import.meta.env.VITE_USE_MOCK !== '1') return;
  const { planningWorker } = await import('@/shared/api/mocks/browser');
  await planningWorker.start({ onUnhandledRequest: 'bypass' });
};
