package com.shiroha.mmdskin.ui.network;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.function.Consumer;

/**
 * 模型选择网络通信处理器
 * 用于在客户端和服务器之间同步模型选择
 */
public class ModelSelectorNetworkHandler {
    private static final Logger logger = LogManager.getLogger();
    private static Consumer<String> networkSender;

    /**
     * 设置网络发送器（由平台特定代码调用）
     */
    public static void setNetworkSender(Consumer<String> sender) {
        networkSender = sender;
        logger.info("模型选择网络发送器已设置");
    }

    /**
     * 发送模型变更到服务器
     */
    public static void sendModelChangeToServer(String modelName) {
        if (networkSender != null) {
            networkSender.accept(modelName);
            logger.info("发送模型变更到服务器: " + modelName);
        } else {
            logger.warn("网络发送器未设置，无法发送模型变更");
        }
    }
}
