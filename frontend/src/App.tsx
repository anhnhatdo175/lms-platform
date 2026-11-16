import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { BrowserRouter } from 'react-router';
import { LoginPage } from './pages/LoginPage';
import './styles/global.css';

const queryClient = new QueryClient();

export default function App() {
  return (
    <BrowserRouter>
      <QueryClientProvider client={queryClient}>
        <LoginPage />
      </QueryClientProvider>
    </BrowserRouter>
  );
}
