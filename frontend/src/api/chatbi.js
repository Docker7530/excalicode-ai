import api from './request.js';

/** ChatBI API */
export const askChatBi = (payload) => api.post('/api/chatbi/ask', payload);

export const listChatBiSessions = () => api.get('/api/chatbi/sessions');

export const getChatBiSessionDetail = (sessionId) =>
  api.get(`/api/chatbi/sessions/${sessionId}`);
