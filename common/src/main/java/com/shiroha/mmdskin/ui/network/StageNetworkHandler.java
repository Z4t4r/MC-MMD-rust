package com.shiroha.mmdskin.ui.network;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.function.Consumer;

/**
 * 舞台模式网络通信处理器
 * 用于在联机时同步舞台动画的开始和结束
 * 
 * opCode 7: 舞台开始 — 字符串格式 "packName|file1.vmd|file2.vmd|..."
 * opCode 8: 舞台结束 — 空字符串
 */
public class StageNetworkHandler {
    private static final Logger logger = LogManager.getLogger();
    
    // 网络发送器（由平台特定代码设置）
    private static Consumer<String> stageStartSender;
    private static Runnable stageEndSender;
    
    /**
     * 设置舞台开始网络发送器
     * @param sender 接收舞台数据字符串 "packName|file1.vmd|..."
     */
    public static void setStageStartSender(Consumer<String> sender) {
        stageStartSender = sender;
        logger.info("舞台模式网络发送器已设置");
    }
    
    /**
     * 设置舞台结束网络发送器
     */
    public static void setStageEndSender(Runnable sender) {
        stageEndSender = sender;
    }
    
    /**
     * 广播舞台开始到服务器
     * @param stageData 格式: "packName|file1.vmd|file2.vmd|..."
     */
    public static void sendStageStart(String stageData) {
        if (stageStartSender != null) {
            stageStartSender.accept(stageData);
            logger.info("广播舞台开始: {}", stageData);
        } else {
            logger.debug("舞台网络发送器未设置，跳过广播（单人模式）");
        }
    }
    
    /**
     * 广播舞台结束到服务器
     */
    public static void sendStageEnd() {
        if (stageEndSender != null) {
            stageEndSender.run();
            logger.info("广播舞台结束");
        } else {
            logger.debug("舞台网络发送器未设置，跳过广播（单人模式）");
        }
    }
}
