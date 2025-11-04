
import { authedFetch } from './utils.js';

export const getRules = async () => {
    const response = await authedFetch('/api/rules');
    if (!response.ok) throw new Error(`HTTP error! status: ${response.status}`);
    return response.json();
};

export const createRule = async (ruleData) => {
    const response = await authedFetch('/api/rules', {
        method: 'POST',
        body: JSON.stringify(ruleData)
    });
    if (!response.ok) {
        const errorText = await response.text();
        throw new Error(errorText || '创建规则失败');
    }
    return response.json();
};

export const updateRule = async (ruleId, ruleData) => {
    const response = await authedFetch(`/api/rules/${ruleId}`, {
        method: 'PUT',
        body: JSON.stringify(ruleData)
    });
    if (!response.ok) {
        const errorText = await response.text();
        throw new Error(errorText || '更新规则失败');
    }
    return response.json();
};

export const deleteRule = async (ruleId) => {
    const response = await authedFetch(`/api/rules/${ruleId}`, { method: 'DELETE' });
    if (!response.ok) {
        const errorText = await response.text();
        throw new Error(errorText || '删除规则失败');
    }
};

export const getAvailableRuleGroups = async () => {
    const rules = await getRules();
    return [...new Set(rules.map(r => r.ruleGroup))];
};
