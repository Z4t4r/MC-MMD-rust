package com.shiroha.mmdskin.ui.network;

/**
 * 动作轮盘网络通信抽象接口
 * 由平台特定代码实现
 */
public class ActionWheelNetworkHandler {
    private static NetworkSender networkSender;

    /**
     * 设置网络发送器（由平台特定代码调用）
     */
    public static void setNetworkSender(NetworkSender sender) {
        networkSender = sender;
    }

    /**
     * 发送动作到服务器
     */
    public static void sendActionToServer(String animId) {
        if (networkSender != null) {
            networkSender.sendAction(animId);
        }
    }

    /**
     * 网络发送器接口
     */
    public interface NetworkSender {
        void sendAction(String animId);
    }
}
