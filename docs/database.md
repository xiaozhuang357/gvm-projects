# 数据库使用说明

本文档介绍 WMS 服务端的数据库存储方式、配置方法以及架构设计。

## 总览

项目采用分层架构设计：

- **配置层** (`config/`): `ConfigManager` 和 `DatabaseConfig` 负责读取和管理数据库配置
- **数据库层** (`db/`): `DatabaseManager` 管理数据库连接，`MysqlConnectionPool` 提供连接池
- **DAO层** (`dao/`): 提供各实体的数据访问接口（`UserDao`, `ProductDao`, `PurchaseOrderDao` 等）
- **服务层** (`service/`): `DataService` 作为业务门面，封装所有数据操作

所有数据存储在 MySQL 数据库中，包括：用户、商品、采购订单及明细、入库/出库记录、操作日志。

## 配置

### 配置文件：`src/main/resources/config.ini`

```ini
[Mysql]
Url = jdbc:mysql://127.0.0.1:3306/
User = root
Passwd = your_password
Schema = wms_db
PoolSize = 10
```

### 参数说明
- `Url`: MySQL 服务器地址
- `User`, `Passwd`: 数据库用户名和密码
- `Schema`: 数据库名称（需存在或具备创建权限）
- `PoolSize`: 连接池大小

### 连接池参数
- `maxWaitTime`: 10000ms
- 事务隔离级别: READ_COMMITTED
- 健康检查间隔: 60 秒

## 架构设计

### 目录结构

```
com._404.wms/
├── config/
│   ├── ConfigManager.java      # 配置管理器
│   └── DatabaseConfig.java     # 数据库配置类
├── db/
│   ├── DatabaseManager.java    # 数据库管理器（单例）
│   ├── connection/
│   │   ├── ConnectionPool.java       # 连接池接口
│   │   └── MysqlConnectionPool.java  # MySQL连接池实现
│   └── util/
│       └── DbUtils.java        # 数据库工具类
├── dao/
│   ├── BaseDao.java            # DAO基础接口
│   ├── UserDao.java            # 用户DAO接口
│   ├── ProductDao.java         # 商品DAO接口
│   ├── PurchaseOrderDao.java   # 采购订单DAO接口
│   ├── StockInRecordDao.java   # 入库记录DAO接口
│   ├── StockOutRecordDao.java  # 出库记录DAO接口
│   ├── OperationLogDao.java    # 操作日志DAO接口
│   ├── DaoFactory.java         # DAO工厂（单例）
│   └── impl/
│       ├── AbstractDao.java          # DAO抽象基类
│       ├── UserDaoImpl.java          # 用户DAO实现
│       ├── ProductDaoImpl.java       # 商品DAO实现
│       ├── PurchaseOrderDaoImpl.java # 采购订单DAO实现
│       ├── StockInRecordDaoImpl.java # 入库记录DAO实现
│       ├── StockOutRecordDaoImpl.java# 出库记录DAO实现
│       └── OperationLogDaoImpl.java  # 操作日志DAO实现
└── service/
    └── DataService.java        # 业务服务层
```

### DAO 工厂模式

通过 `DaoFactory` 获取各 DAO 实例：

```java
DaoFactory factory = DaoFactory.getInstance();
UserDao userDao = factory.getUserDao();
ProductDao productDao = factory.getProductDao();
// ...
```

## 数据库表结构

启动时由 `DatabaseManager` 自动创建：

### users 表
- 用户信息，用户名唯一
- 含激活标记、登录时间等

### products 表
- 商品信息
- 含当前库存、安全库存、上限
- 采购价、销售价、供应商、启用状态

### purchase_orders 表
- 采购单表头
- 状态以文本存储
- 含金额、审批人、各时间戳、期望交付日

### purchase_order_items 表
- 采购单明细
- 外键关联 `purchase_orders`

### stock_in_records 表
- 入库记录
- 含批次、仓库位置、操作员、时间戳

### stock_out_records 表
- 出库记录
- 含领用人、部门、用途、操作员、时间戳

### operation_logs 表
- 操作日志
- 含成功标志和错误信息

## 典型数据流程

### 登录
`UserDao.authenticate` 验证用户名密码，检查 `active=true`

### 商品管理
通过 `ProductDao` 进行 CRUD 操作；低库存查询使用 `current_stock <= min_stock`

### 采购订单
`PurchaseOrderDao` 支持事务性写入表头+明细，状态与时间戳持久化

### 入/出库
按时间倒序查询；记录含操作员与备注

### 日志
写入 `operation_logs` 表，ID 自动生成

## 运维检查清单

1. 确认 MySQL 可连且 `Schema` 已存在
2. 启动服务，确认控制台出现初始化成功信息
3. 若 `users` 为空，先插入至少一条用户数据
4. 端到端验证：登录 → 新增商品 → 创建/审批采购单 → 入库/出库 → 查看日志

## 启动方式

### Windows
```batch
# 使用批处理脚本
start_server.bat

# 或使用 Maven
mvnw.cmd javafx:run
```

## 备注

- 所有 ID 使用 `VARCHAR`，时间列使用 `DATETIME`
- 明细表使用自增 `id` 作为主键
- 连接池定期健康检查，失效连接会异步重建
