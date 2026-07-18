import './styles/main.css';
import { renderShell } from './components/shell';
import { renderDashboard } from './pages/dashboard';
import { renderTriage } from './pages/triage';
import { renderEmergency } from './pages/emergency';
import { renderCoordination } from './pages/coordination';
import { renderAnalytics } from './pages/analytics';

const routes: Record<string, (el: HTMLElement) => void | Promise<void>> = {
  dashboard: renderDashboard,
  triage: renderTriage,
  emergency: renderEmergency,
  coordination: renderCoordination,
  analytics: renderAnalytics,
};

const root = document.getElementById('app')!;

function currentRoute(): string {
  const hash = window.location.hash.replace('#', '');
  return routes[hash] ? hash : 'dashboard';
}

function navigate() {
  const routeId = currentRoute();
  const content = renderShell(root, routeId);
  const handler = routes[routeId];
  void handler(content);
}

window.addEventListener('hashchange', navigate);
navigate();
