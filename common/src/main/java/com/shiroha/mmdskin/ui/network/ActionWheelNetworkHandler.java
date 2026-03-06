package com.shiroha.mmdskin.ui.network;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.function.Consumer;

/**
 * 动作轮盘网络通信处理器
 * 负责将动作选择同步到服务器
 */
public class ActionWheelNetworkHandler {
    private static final Logger logger = LogManager.getLogger();
    private static Consumer<String> networkSender;

    /**
     * 设置网络发送器（由平台特定代码调用）
     */
    public static void setNetworkSender(Consumer<String> sender) {
        networkSender = sender;
        logger.info("动作轮盘网络发送器已设置");
    }

    /**
     * 发送动作到服务器
     */
    public static void sendActionToServer(String animId) {
        if (networkSender != null) {
            try {
                networkSender.accept(animId);
                logger.debug("发送动作到服务器: {}", animId);
            } catch (Exception e) {
                logger.error("发送动作失败", e);
            }
        }
    }
}
