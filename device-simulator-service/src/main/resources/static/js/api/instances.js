
import { authedFetch } from './utils.js';

export const getProfileStatuses = async () => {
    const response = await authedFetch('/api/instances');
    if (!response.ok) throw new Error(`HTTP error! status: ${response.status}`);
    return response.json();
};

export const startInstance = async (profileName) => {
    const response = await authedFetch(`/api/instances/${profileName}/_start`, { method: 'POST' });
    if (!response.ok) {
        const errorText = await response.text();
        throw new Error(errorText || '启动实例失败');
    }
    return response.json();
};

export const stopInstance = async (profileName) => {
    const response = await authedFetch(`/api/instances/${profileName}/_stop`, { method: 'POST' });
    if (!response.ok) {
        const errorText = await response.text();
        throw new Error(errorText || '停止实例失败');
    }
    return response.json();
};
