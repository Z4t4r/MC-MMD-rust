package com.shiroha.mmdskin.ui.network;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.function.Consumer;

/**
 * 表情轮盘网络处理器
 * 负责将表情选择同步到服务器
 */
public class MorphWheelNetworkHandler {
    private static final Logger logger = LogManager.getLogger();
    
    private static Consumer<String> networkSender = null;
    
    /**
     * 设置网络发送器（由平台特定代码注册）
     */
    public static void setNetworkSender(Consumer<String> sender) {
        networkSender = sender;
    }
    
    /**
     * 发送表情到服务器
     * @param morphName 表情名称
     */
    public static void sendMorphToServer(String morphName) {
        if (networkSender != null) {
            try {
                networkSender.accept(morphName);
                logger.debug("发送表情到服务器: {}", morphName);
            } catch (Exception e) {
                logger.error("发送表情失败", e);
            }
        }
    }
}
