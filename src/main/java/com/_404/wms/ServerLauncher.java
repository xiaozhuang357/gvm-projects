package com._404.wms;

import com._404.wms.network.WMSServer;

/**
 * WMS仓库管理系统 - 服务端启动类
 * <p>
 * 功能说明：
 * 1. 无需JavaFX图形界面，适合部署在无图形环境的服务器上
 * 2. 启动Socket服务端，监听客户端连接请求
 * 3. 初始化数据库连接池和数据服务
 * 4. 处理客户端的业务请求（用户管理、商品管理、库存管理等）
 * 5. 优雅关闭：通过ShutdownHook确保资源正确释放
 * <p>
 * 使用方式：
 * - 命令行运行：java -jar WMS-Server.jar
 * - 后台运行：nohup java -jar WMS-Server.jar > wms.log 2>&1 &
 * <p>
 * 配置文件：config.ini
 * - [Server] Host和Port：服务端监听地址和端口
 * - [Mysql] 数据库连接配置
 *
 * @author WMS开发团队
 * @version 1.0
 * @since 2025-12-06
 */
public class ServerLauncher {

    /**
     * 服务端主入口方法
     * <p>
     * 执行流程：
     * 1. 打印启动信息
     * 2. 创建WMSServer实例（自动读取配置文件）
     * 3. 注册JVM关闭钩子，确保服务器优雅退出
     * 4. 调用server.start()启动服务（阻塞式运行）
     * 5. 捕获并处理启动异常
     *
     * @param args 命令行参数（当前未使用）
     */
    public static void main(String[] args) {
        System.out.println("==========================================");
        System.out.println("      WMS 仓库管理系统 - 服务端启动      ");
        System.out.println("==========================================");

        try {
            // 创建WMSServer实例
            // 会自动完成：配置加载、数据库初始化、连接池创建、DAO层初始化
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
