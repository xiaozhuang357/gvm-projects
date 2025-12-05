# 仓库管理系统 (WMS)

一个基于JavaFX和Socket通信的分布式仓库管理系统，支持用户管理、商品管理、采购管理、出入库管理、报表统计等完整功能。

## 主要特性

✅ **多用户角色**: 仓库管理员、采购员、部门经理、总经理  
✅ **Socket通信**: 客户端-服务器架构，支持多客户端并发  
✅ **完整业务流程**: 采购订单→逐级审批→入库→出库  
✅ **权限控制**: 基于角色的访问控制  
✅ **数据持久化**: 自动保存到本地文件  
✅ **操作日志**: 记录所有用户操作  
✅ **报表导出**: 支持Excel/CSV/PDF格式  
✅ **库存预警**: 低库存自动提醒  
✅ **数据备份**: 支持备份和恢复  

## 技术栈

- **UI框架**: JavaFX
- **网络通信**: Socket (TCP/IP)
- **数据存储**: Java对象序列化
- **并发处理**: ExecutorService + 线程池
- **构建工具**: Maven

## 项目结构

```
WMS/
├── src/main/
│   ├── java/com/_404/wms/
│   │   ├── model/              # 数据模型
│   │   │   ├── User.java
│   │   │   ├── Product.java
│   │   │   ├── PurchaseOrder.java
│   │   │   ├── StockInRecord.java
│   │   │   ├── StockOutRecord.java
│   │   │   └── OperationLog.java
│   │   │
│   │   ├── network/            # 网络通信
│   │   │   ├── Message.java
│   │   │   ├── WMSServer.java
│   │   │   └── SocketClient.java
│   │   │
│   │   ├── service/            # 业务逻辑
│   │   │   └── DataService.java
│   │   │
│   │   ├── LoginController.java       # 登录控制器
│   │   ├── ManagerController.java     # 经理界面控制器
│   │   ├── LoginApplication.java
│   │   └── Launcher.java
│   │
│   └── resources/com/_404/wms/
│       ├── login.fxml          # 登录界面
│       └── manager.fxml        # 经理工作台界面
│
├── wms_data/                   # 数据存储目录 (自动创建)
│   ├── users.dat
│   ├── products.dat
│   ├── orders.dat
│   ├── stock_in.dat
│   ├── stock_out.dat
│   └── operation.log
│
├── pom.xml                     # Maven配置
├── 系统使用手册.md
└── README.md
```

## 快速开始

### 前置条件

- JDK 17 或更高版本
- Maven 3.6+ (可选)

### 1. 启动服务器

#### 方式一：IDE运行 (推荐)
1. 打开项目
2. 找到 `WMSServer.java`
3. 右键 → Run 'WMSServer.main()'

#### 方式二：命令行运行
```bash
# Windows
cd "d:\Code\IDEA code\WMS"
javac -d target/classes -encoding UTF-8 -cp "src/main/java" src/main/java/com/_404/wms/network/WMSServer.java src/main/java/com/_404/wms/model/*.java src/main/java/com/_404/wms/service/*.java
java -cp target/classes com._404.wms.network.WMSServer
```

**服务器启动成功标志:**
```
WMS服务器已启动，监听端口: 8888
等待客户端连接...
示例数据初始化完成
```

### 2. 启动客户端

#### 方式一：IDE运行 (推荐)
1. 找到 `Launcher.java`
2. 右键 → Run 'Launcher.main()'

#### 方式二：使用Maven
```bash
mvn clean javafx:run
```

### 3. 登录系统

使用以下测试账号登录：

| 角色       | 用户名    | 密码     | 权限说明         |
| ---------- | --------- | -------- | ---------------- |
| 仓库管理员 | admin     | admin123 | 全部权限         |
| 部门经理   | manager1  | 123      | 审批<50000元订单 |
| 总经理     | general   | 123      | 审批≥50000元订单 |
| 采购员     | purchaser | 123      | 创建采购订单     |

## 核心功能演示

### 1. 用户管理
- 登录/登出
- 多角色切换
- 权限控制

### 2. 商品管理
- 添加/编辑/删除商品
- 库存查询 (按类别、库存上下限)
- 库存调整 (盘点、修正)
- 库存预警

### 3. 采购管理
```
采购流程:
1. 采购员创建订单
2. 系统根据金额自动分配审批人
   - < 50,000元 → 部门经理
   - ≥ 50,000元 → 总经理
3. 审批人审批/退回
4. 订单跟踪
```

### 4. 出入库管理
- 入库登记 (批次、日期、位置)
- 出库登记 (领用人、部门、用途)
- 自动更新库存
- 库存预警

### 5. 报表统计
- 库存报表 (分类、价值、周转率)
- 采购报表 (金额、到货率)
- 可视化图表 (柱状图、折线图、饼图)
- 报表导出 (Excel/CSV/PDF)

### 6. 系统安全
- 操作日志记录
- 数据备份与恢复
- 用户权限控制

## Socket通信协议

### 消息格式
```java
{
    "type": "LOGIN_REQUEST",
    "sender": "user001",
    "data": { "username": "admin", "password": "admin123" },
    "success": true,
    "message": "登录成功",
    "timestamp": 1234567890
}
```

### 主要消息类型
- **认证**: LOGIN_REQUEST, LOGIN_RESPONSE, LOGOUT
- **用户**: USER_ADD, USER_UPDATE, USER_DELETE, USER_LIST
- **商品**: PRODUCT_ADD, PRODUCT_UPDATE, PRODUCT_DELETE, PRODUCT_LIST
- **采购**: PURCHASE_ORDER_CREATE, PURCHASE_ORDER_APPROVE, PURCHASE_ORDER_REJECT
- **出入库**: STOCK_IN, STOCK_OUT, STOCK_IN_LIST, STOCK_OUT_LIST
- **报表**: REPORT_INVENTORY, REPORT_PURCHASE, REPORT_EXPORT
- **日志**: LOG_LIST, LOG_QUERY
- **系统**: BACKUP, RESTORE, HEARTBEAT

详见: [系统使用手册.md](系统使用手册.md)

## 数据存储

系统使用Java对象序列化存储数据，所有数据保存在 `wms_data/` 目录：

```
wms_data/
├── users.dat           # 用户数据
├── products.dat        # 商品数据
├── orders.dat          # 采购订单
├── stock_in.dat        # 入库记录
├── stock_out.dat       # 出库记录
└── operation.log       # 操作日志 (文本格式)
```

**数据备份**: 系统支持一键备份和恢复所有数据

## 开发指南

### 添加新功能

1. **创建数据模型** (model包)
```java
public class NewEntity implements Serializable {
    // 属性和方法
}
```

2. **添加消息类型** (Message.java)
```java
public enum MessageType {
    NEW_FEATURE_REQUEST,
    NEW_FEATURE_RESPONSE
}
```

3. **服务器端处理** (WMSServer.java)
```java
private Message handleNewFeature(Message message) {
    // 业务逻辑
    return Message.success(type, data, "操作成功");
}
```

4. **客户端调用** (Controller)
```java
Message request = new Message(MessageType.NEW_FEATURE_REQUEST, data);
Message response = socketClient.sendAndReceive(request);
```

### 创建新角色界面

1. 创建FXML文件: `src/main/resources/com/_404/wms/new_role.fxml`
2. 创建Controller: `src/main/java/com/_404/wms/NewRoleController.java`
3. 在LoginController中添加路由逻辑

## 常见问题

### Q: 服务器启动失败，端口被占用？
```bash
# 查找占用8888端口的进程
netstat -ano | findstr 8888

# 结束进程 (PID是上面查到的进程ID)
taskkill /F /PID <PID>
```

### Q: 客户端无法连接服务器？
1. 确认服务器已启动
2. 检查防火墙设置
3. 确认端口8888未被占用
4. 查看服务器日志

### Q: 数据丢失怎么办？
使用系统的数据恢复功能，从备份目录恢复

### Q: 如何修改服务器地址？
修改 `SocketClient.java` 中的常量:
```java
private static final String SERVER_HOST = "your_server_ip";
private static final int SERVER_PORT = 8888;
```

## 性能优化

- 使用线程池处理并发连接
- 数据异步加载
- 界面更新使用Platform.runLater()
- Socket保持长连接，减少握手开销

## 系统扩展

### 扩展到远程服务器
1. 修改SERVER_HOST为服务器IP
2. 配置服务器防火墙规则
3. 考虑使用SSL/TLS加密通信

### 集成数据库
将DataService改为使用JDBC:
1. 添加数据库依赖 (MySQL/PostgreSQL)
2. 创建数据表
3. 修改DataService实现

### 添加更多图表
使用JavaFX Charts API:
- XYChart (折线图、柱状图、面积图)
- PieChart (饼图)
- ScatterChart (散点图)

## 贡献

欢迎提交Issue和Pull Request！

## 许可证

MIT License

## 作者

_404 Team

## 版本历史

- **v1.0.0** (2025-12-04)
  - 初始版本
  - 实现核心功能
  - Socket通信
  - 多角色支持
  - 报表导出

---

📖 详细文档: [系统使用手册.md](系统使用手册.md)

🚀 快速开始: 启动WMSServer → 启动Launcher → 使用admin/admin123登录

💡 技术支持: _404 Team
