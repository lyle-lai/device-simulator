
import { authedFetch } from './utils.js';

export const getProfiles = async () => {
    const response = await authedFetch('/api/profiles');
    if (!response.ok) throw new Error(`HTTP error! status: ${response.status}`);
    return response.json();
};

export const getProfile = async (profileName) => {
    const response = await authedFetch(`/api/profiles/${profileName}`);
    if (!response.ok) throw new Error(`HTTP error! status: ${response.status}`);
    return response.json();
};

export const createProfile = async (profileData) => {
    const response = await authedFetch('/api/profiles', {
        method: 'POST',
        body: JSON.stringify(profileData)
    });
    if (!response.ok) {
        const errorText = await response.text();
        throw new Error(errorText || '创建画像失败');
    }
    return response.json();
};

export const updateProfile = async (profileName, profileData) => {
    const response = await authedFetch(`/api/profiles/${profileName}`, {
        method: 'PUT',
        body: JSON.stringify(profileData)
    });
    if (!response.ok) {
        const errorText = await response.text();
        throw new Error(errorText || '更新画像失败');
    }
    return response.json();
};

export const deleteProfile = async (profileName) => {
    const response = await authedFetch(`/api/profiles/${profileName}`, { method: 'DELETE' });
    if (!response.ok) {
        const errorText = await response.text();
        throw new Error(errorText || '删除画像失败');
    }
};
