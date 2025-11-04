
// 检查并处理认证失败的通用逻辑
const handleAuthFailure = (response) => {
    if (response.status === 401 || response.status === 403) {
        // 抛出特定错误，以便在调用处捕获并处理
        throw new Error('AUTH_FAILURE');
    }
};

// 封装通用 fetch 请求
export const authedFetch = async (url, options = {}) => {
    const headers = {
        'Content-Type': 'application/json',
        ...options.headers,
    };

    const token = localStorage.getItem('jwt');
    if (token) {
        headers['Authorization'] = `Bearer ${token}`;
    }

    const finalOptions = {
        ...options,
        headers,
    };

    const response = await fetch(url, finalOptions);
    handleAuthFailure(response);

    return response;
};
