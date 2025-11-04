import { Client } from '@stomp/stompjs';

class WebSocketService {
    constructor() {
        this.client = null;
    }

    connect(onConnected, onError) {
        // 如果客户端已连接，则直接执行回调，避免重复连接
        if (this.client && this.client.active) {
            if (onConnected) onConnected();
            return;
        }

        // 构造WebSocket URL。在开发环境中，Vite会代理/ws请求。
        // 在生产环境中，它会连接到提供前端文件的同一主机。
        const wsProtocol = window.location.protocol === 'https:' ? 'wss:' : 'ws:';
        const brokerURL = `${wsProtocol}//${window.location.host}/ws`;

        this.client = new Client({
            brokerURL: brokerURL,
            reconnectDelay: 5000, // 自动重连延迟
            debug: (str) => {
                // console.log(new Date(), str); // 取消注释以进行详细调试
            },
            onConnect: (frame) => {
                console.log('STOMP connected:', frame);
                if (onConnected) {
                    onConnected();
                }
            },
            onStompError: (frame) => {
                console.error('STOMP error:', frame.headers['message'], frame.body);
                if (onError) {
                    onError(frame);
                }
            },
            onWebSocketError: (error) => {
                console.error('WebSocket error:', error);
                if (onError) {
                    onError(error);
                }
            }
        });

        this.client.activate();
    }

    disconnect() {
        if (this.client) {
            this.client.deactivate();
            this.client = null;
            console.log('STOMP disconnected.');
        }
    }

    subscribe(topic, callback) {
        if (this.client && this.client.active) {
            // subscribe方法返回一个带有unsubscribe方法的对象
            return this.client.subscribe(topic, message => {
                callback(JSON.parse(message.body));
            });
        }
        console.error('无法订阅，STOMP客户端未激活。');
        return null;
    }
}

export const webSocketService = new WebSocketService();

