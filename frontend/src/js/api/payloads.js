
import { authedFetch } from './utils.js';

export const getPayloads = async () => {
    const response = await authedFetch('/api/payloads');
    if (!response.ok) throw new Error(`HTTP error! status: ${response.status}`);
    return response.json();
};

export const createPayload = async (payloadData) => {
    const response = await authedFetch('/api/payloads', {
        method: 'POST',
        body: JSON.stringify(payloadData)
    });
    if (!response.ok) {
        const errorText = await response.text();
        throw new Error(errorText || '创建报文失败');
    }
    return response.json();
};

export const updatePayload = async (payloadKey, payloadData) => {
    const response = await authedFetch(`/api/payloads/${payloadKey}`, {
        method: 'PUT',
        body: JSON.stringify(payloadData)
    });
    if (!response.ok) {
        const errorText = await response.text();
        throw new Error(errorText || '更新报文失败');
    }
    return response.json();
};

export const deletePayload = async (payloadKey) => {
    const response = await authedFetch(`/api/payloads/${payloadKey}`, { method: 'DELETE' });
    if (!response.ok) {
        const errorText = await response.text();
        throw new Error(errorText || '删除报文失败');
    }
};

export const getAvailablePayloadGroupKeys = async () => {
    const payloads = await getPayloads();
    return [...new Set(payloads.map(p => p.groupKey))].filter(key => key);
};

export const getAvailablePayloadKeys = async () => {
    const payloads = await getPayloads();
    return payloads.map(p => p.payloadKey);
};
