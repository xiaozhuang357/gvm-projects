package com._404.wms;

import javafx.application.Application;

/**
 * WMS仓库管理系统 - 客户端启动类
 * <p>
 * 功能说明：
 * 1. 启动JavaFX图形界面应用程序
 * 2. 首先显示登录界面（LoginApplication）
 * 3. 登录成功后根据用户角色跳转到对应的功能界面：
 * - 仓库管理员：WarehouseAdminController（商品管理、库存管理、用户管理、报表统计）
 * - 采购员：PurchaserController（采购订单创建、查询）
 * - 部门经理/总经理：ManagerController（订单审批）
 * <p>
 * 技术架构：
 * - UI框架：JavaFX 21
 * - 网络通信：Socket客户端（SocketClient）
 * - 数据传输：序列化对象（Message封装）
 * - 配置管理：ConfigManager读取config.ini
 * <p>
 * 使用方式：
 * - IDE运行：直接运行main方法
 * - 命令行：java -jar WMS-Client.jar
 * <p>
 * 配置文件：config.ini
 * - [Server] Host和Port：指定服务端地址和端口
 * - [Mysql] 本地数据库配置（可选，仅客户端缓存使用）
 *
 * @author WMS开发团队
 * @version 1.0
 * @since 2025-12-06
 */
public class Launcher {

	/**
	 * 客户端主入口方法
	 * <p>
	 * 通过JavaFX的Application.launch()方法启动图形界面
	 * 会自动调用LoginApplication类的start()方法显示登录窗口
	 *
	 * @param args 命令行参数（传递给JavaFX应用）
	 */
	public static void main(String[] args) {
		// 启动JavaFX应用，加载登录界面
		Application.launch(LoginApplication.class, args);
	}
}
