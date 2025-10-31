
class WebSocketService {
    constructor() {
        this.stompClient = null;
        this.subscriptions = {}; // Store subscriptions by topic
    }

    connect(onConnected, onError) {
        // 假设 SockJS 和 Stomp 已经作为全局变量可用
        const socket = new SockJS('/ws');
        this.stompClient = Stomp.over(socket);
        this.stompClient.debug = null; // Disable STOMPJS console logs
        this.stompClient.connect({}, frame => {
            console.log('Connected to WebSocket: ' + frame);
            onConnected();
        }, error => {
            console.error('WebSocket connection error:', error);
            onError(error);
        });
    }

    disconnect() {
        if (this.stompClient && this.stompClient.connected) {
            this.stompClient.disconnect(() => {
                console.log('Disconnected from WebSocket');
                this.stompClient = null;
                this.subscriptions = {};
            });
        }
    }

    subscribe(topic, callback) {
        if (this.stompClient && this.stompClient.connected) {
            const subscription = this.stompClient.subscribe(topic, message => {
                callback(JSON.parse(message.body));
            });
            this.subscriptions[topic] = subscription;
            return subscription;
        } else {
            console.warn('WebSocket not connected. Cannot subscribe to topic:', topic);
            return null;
        }
    }

    unsubscribe(topic) {
        if (this.subscriptions[topic]) {
            this.subscriptions[topic].unsubscribe();
            delete this.subscriptions[topic];
            console.log('Unsubscribed from topic:', topic);
        }
    }
}

export const webSocketService = new WebSocketService();
