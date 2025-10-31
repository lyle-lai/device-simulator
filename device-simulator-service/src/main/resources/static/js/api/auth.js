
import { authedFetch } from './utils.js';

export const login = async (username, password) => {
    const response = await fetch('/api/auth/login', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ username, password })
    });
    if (response.ok) {
        return response.json();
    } else {
        const errorText = await response.text();
        throw new Error(errorText || '登录失败');
    }
};

export const changePassword = async (passwords) => {
    return authedFetch('/api/user/change-password', {
        method: 'POST',
        body: JSON.stringify(passwords)
    });
};
