package com._404.wms;

import com._404.wms.network.WMSServer;

/**
 * 服务端启动类（无需图形界面）
 */
public class ServerLauncher {
    public static void main(String[] args) {
        System.out.println("==========================================");
        System.out.println("      WMS 仓库管理系统 - 服务端启动      ");
        System.out.println("==========================================");

        try {
            // 创建并启动服务器
            WMSServer server = new WMSServer();

            // 添加关闭钩子，优雅退出
            Runtime.getRuntime().addShutdownHook(new Thread(() -> {
                System.out.println("\n正在关闭服务器...");
                server.stop();
            }));

            // 启动服务器（阻塞运行）
            System.out.println("正在启动服务器...");
            server.start();
            System.out.println("服务器已退出");
        } catch (Exception e) {
            System.err.println("服务器启动失败: " + e.getMessage());
            e.printStackTrace();
            System.exit(1);
        }
    }
}
