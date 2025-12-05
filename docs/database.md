# 数据库使用说明

本文档介绍 WMS 服务端的数据库存储方式、配置方法以及运行时行为。

## 总览
- 主要组件：`MysqlMgr`（数据库访问与建表）、`MysqlPool`（连接池）、`DataService`（业务门面，优先写库，DB 不可用时回退本地文件）。
- 存入 MySQL 的数据：用户、商品、采购订单及明细、入库/出库记录、操作日志。数据库不可用时才写本地文件。
- 启动时 `MysqlMgr` 读取 `config.ini`，初始化连接池并自动创建表。

## 配置
- 配置文件：`src/main/resources/config.ini`
- `[Mysql]` 下的键：
  - `Url` 例如 `jdbc:mysql://127.0.0.1:3306/`
  - `User`, `Passwd`
  - `Schema` 例如 `wms_db`（需存在或具备创建权限）
  - `PoolSize` 例如 `10`
- 连接池固定参数：`maxWaitTime` 10000ms；事务隔离 READ_COMMITTED；健康检查每 60 秒。

## 表结构（自动创建）
- `users`：用户名唯一，含激活标记、登录时间等。
- `products`：商品信息，含当前库存/安全库存/上限、采购价/销售价、供应商、启用状态。
- `purchase_orders`：采购单表头（状态以文本存储），含金额、审批人、各时间戳、期望交付日。
- `purchase_order_items`：采购单明细，`order_id` 索引。
- `stock_in_records`：入库记录，含批次、仓库、操作员、时间戳。
- `stock_out_records`：出库记录，含领用人、部门、用途、操作员、时间戳。
- `operation_logs`：操作日志，含成功标志和错误信息。

## 运行时行为
- `DataService` 在 `MysqlMgr` 初始化成功后设置 `dbEnabled=true`。
- `dbEnabled=true`：商品、采购单、库存记录、日志全部写 MySQL，不落本地文件。
- `dbEnabled=false`（数据库不可达或配置无效）：回退到 `wms_data/` 下的序列化文件，不写库。
- 用户数据始终从 MySQL 获取（登录、查询等）。

## 典型数据流程
- 登录：`MysqlMgr.authenticate` 检查 `users` 且需 `active=true`。
- 商品：CRUD 落 `products`；低库存查询在 DB 模式使用 `current_stock <= min_stock`。
- 采购单：表头+明细事务写入/更新，状态与时间戳持久化。
- 入/出库：按时间倒序查询；记录含操作员与备注。
- 日志：写入前若无 ID 会生成 `LOG+时间戳`；DB 模式写入 `operation_logs`，文件仅在 DB 失效时使用。

## 运维检查清单
1) 确认 MySQL 可连且 `Schema` 已存在（或账号有创建权限）；可用配置中的 `Url` 先做一次连通性测试。
2) 启动服务，确认控制台出现 “MySQL Manager initialized successfully” 与 “Database tables initialized successfully”。
3) 若 `users` 为空，先插入至少一条用户数据以便登录。
4) 做端到端验证：登录 → 新增商品 → 创建/审批采购单 → 入库/出库 → 查看日志。

## 常见恢复场景
- 数据库临时不可用：程序自动回文件模式；DB 恢复后重启以重新启用 `dbEnabled`。
- 需要把历史文件数据导入库：可编写一次性脚本读取 `wms_data/` 后调用 `DataService` 的写库方法，或直接 SQL 导入。

## 便捷信息
- JDBC 示例：`jdbc:mysql://127.0.0.1:3306/`
- 启动（Windows）：可在 IDE 运行 `WMSServer.main`，或使用 `mvnw.cmd` 相关命令（视项目入口而定）。

## 备注
- 所有 ID 使用 `VARCHAR`，时间列使用 `DATETIME`，明细表 `purchase_order_items` 使用自增 `id`。
- 连接池定期健康检查，失效连接会异步重建。
- 日志在 DB 模式只写库，`operation.log` 仅在无数据库时使用。
