package com.xinsec.devicesimulator.service.protocols;

import com.fasterxml.jackson.databind.JsonNode;

/**
 * 协议处理器接口
 * 职责: 处理网络通信的底层细节,只负责发送和接收字节流(byte[]).
 */
public interface ProtocolHandler {

    /**
     * 消息监听器,用于从协议层接收数据
     */
    interface MessageListener {
        /**
         * 当收到消息时调用
         * @param context 连接上下文 (例如 Netty的 ChannelHandlerContext), 用于响应
         * @param data 收到的字节数据
         */
        void onMessageReceived(Object context, byte[] data);
    }

    /**
     * 配置协议处理器
     * @param config protocol部分的JSON配置
     */
    void configure(JsonNode config);

    /**
     * 启动协议处理器,开始连接并准备接收数据
     * @param listener 一个监听器来异步处理接收到的数据
     */
    void start(MessageListener listener);

    /**
     * 停止协议处理器,断开连接
     */
    void stop();

    /**
     * 发送数据 (通常用于客户端或广播)
     * @param data 要发送的字节数组
     */
    void send(byte[] data);

    /**
     * 向指定的连接上下文发送数据 (主要用于服务端响应)
     * @param context 连接上下文
     * @param data 要发送的字节数组
     */
    void send(Object context, byte[] data);
}