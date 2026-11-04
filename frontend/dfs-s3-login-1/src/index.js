import React from 'react';
import ReactDOM from 'react-dom/client';
import './index.css';
import App from './App';
import reportWebVitals from './reportWebVitals';
import { PublicClientApplication } from '@azure/msal-browser';
import { MsalProvider } from '@azure/msal-react';
import { msalConfig } from './authConfig';
import 'bootstrap/dist/css/bootstrap.min.css';


const msalInstance = new PublicClientApplication(msalConfig);

const root = ReactDOM.createRoot(document.getElementById('root'));

// disponibilizando o msal para el proyecto
root.render(
  <React.StrictMode>

    <MsalProvider instance={msalInstance}>
      <App />
    </MsalProvider>

  </React.StrictMode>
);
reportWebVitals();
