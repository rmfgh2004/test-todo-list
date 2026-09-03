import { StrictMode } from 'react';
import { createRoot } from 'react-dom/client';
import { QueryClientProvider } from '@tanstack/react-query';
import { App } from './app/App';
import { createPlanningQueryClient } from './shared/api/cache';
import { connectivityRuntime } from './shared/api/connectivity';
import { installMockTransport } from './shared/api/mocks/install';

const container = document.getElementById('root');
if (container === null) {
  throw new Error('Root container #root is missing from index.html');
}

const queryClient = createPlanningQueryClient();
connectivityRuntime.configurePoll(() => queryClient.refetchQueries({ type: 'active' }));

const render = () =>
  createRoot(container).render(
    <StrictMode>
      <QueryClientProvider client={queryClient}>
        <App live />
      </QueryClientProvider>
    </StrictMode>,
  );

void installMockTransport().then(render);
