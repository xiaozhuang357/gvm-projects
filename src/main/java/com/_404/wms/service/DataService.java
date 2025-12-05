package com._404.wms.service;

import com._404.wms.model.*;

import java.io.*;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * 数据服务类 - 管理所有业务数据
 */
public class DataService {
    private Map<String, User> users;
    private Map<String, Product> products;
    private Map<String, PurchaseOrder> purchaseOrders;
    private List<StockInRecord> stockInRecords;
    private List<StockOutRecord> stockOutRecords;
    private List<OperationLog> logs;

    private static final String DATA_DIR = "wms_data/";

    public DataService() {
        this.users = new ConcurrentHashMap<>();
        this.products = new ConcurrentHashMap<>();
        this.purchaseOrders = new ConcurrentHashMap<>();
        this.stockInRecords = Collections.synchronizedList(new ArrayList<>());
        this.stockOutRecords = Collections.synchronizedList(new ArrayList<>());
        this.logs = Collections.synchronizedList(new ArrayList<>());

        // 创建数据目录
        new File(DATA_DIR).mkdirs();

        // 尝试加载数据
        loadData();
    }

    // ==================== 用户管理 ====================

    /**
     * 用户认证
     */
    public User authenticate(String username, String password) {
        return users.values().stream()
                .filter(u -> u.getUsername().equals(username) &&
                        u.getPassword().equals(password) &&
                        u.isActive())
                .findFirst()
                .orElse(null);
    }

    /**
     * 添加用户
     */
    public void addUser(User user) {
        users.put(user.getUserId(), user);
        saveData();
    }

    /**
     * 更新用户
     */
    public void updateUser(User user) {
        users.put(user.getUserId(), user);
        saveData();
    }

    /**
     * 删除用户
     */
    public void deleteUser(String userId) {
        users.remove(userId);
        saveData();
    }

    /**
     * 根据ID获取用户
     */
    public User getUserById(String userId) {
        return users.get(userId);
    }

    /**
     * 获取所有用户
     */
    public List<User> getAllUsers() {
        return new ArrayList<>(users.values());
    }

    // ==================== 商品管理 ====================

    /**
     * 添加商品
     */
    public void addProduct(Product product) {
        products.put(product.getProductId(), product);
        saveData();
    }

    /**
     * 更新商品
     */
    public void updateProduct(Product product) {
        products.put(product.getProductId(), product);
        saveData();
    }

    /**
     * 删除商品
     */
    public void deleteProduct(String productId) {
        products.remove(productId);
        saveData();
    }

    /**
     * 根据ID获取商品
     */
    public Product getProductById(String productId) {
        return products.get(productId);
    }

    /**
     * 获取所有商品
     */
    public List<Product> getAllProducts() {
        return new ArrayList<>(products.values());
    }

    /**
     * 按类别查询商品
     */
    public List<Product> getProductsByCategory(String category) {
        return products.values().stream()
                .filter(p -> p.getCategory().equals(category))
                .collect(Collectors.toList());
    }

    /**
     * 查询需要补货的商品
     */
    public List<Product> getLowStockProducts() {
        return products.values().stream()
                .filter(Product::needsStockAlert)
                .collect(Collectors.toList());
    }

    // ==================== 采购订单管理 ====================

    /**
     * 添加采购订单
     */
    public void addPurchaseOrder(PurchaseOrder order) {
        purchaseOrders.put(order.getOrderId(), order);
        saveData();
    }

    /**
     * 更新采购订单
     */
    public void updatePurchaseOrder(PurchaseOrder order) {
        purchaseOrders.put(order.getOrderId(), order);
        saveData();
    }

    /**
     * 根据ID获取采购订单
     */
    public PurchaseOrder getPurchaseOrderById(String orderId) {
        return purchaseOrders.get(orderId);
    }

    /**
     * 获取所有采购订单
     */
    public List<PurchaseOrder> getAllPurchaseOrders() {
        return new ArrayList<>(purchaseOrders.values());
    }

    /**
     * 根据状态查询订单
     */
    public List<PurchaseOrder> getOrdersByStatus(PurchaseOrder.OrderStatus status) {
        return purchaseOrders.values().stream()
                .filter(o -> o.getStatus() == status)
                .collect(Collectors.toList());
    }

    /**
     * 根据采购员查询订单
     */
    public List<PurchaseOrder> getOrdersByPurchaser(String purchaserId) {
        return purchaseOrders.values().stream()
                .filter(o -> o.getPurchaserId().equals(purchaserId))
                .collect(Collectors.toList());
    }

    // ==================== 出入库管理 ====================

    /**
     * 添加入库记录
     */
    public void addStockInRecord(StockInRecord record) {
        stockInRecords.add(record);
        saveData();
    }

    /**
     * 获取所有入库记录
     */
    public List<StockInRecord> getAllStockInRecords() {
        return new ArrayList<>(stockInRecords);
    }

    /**
     * 添加出库记录
     */
    public void addStockOutRecord(StockOutRecord record) {
        stockOutRecords.add(record);
        saveData();
    }

    /**
     * 获取所有出库记录
     */
    public List<StockOutRecord> getAllStockOutRecords() {
        return new ArrayList<>(stockOutRecords);
    }

    // ==================== 日志管理 ====================

    /**
     * 添加操作日志
     */
    public void addLog(OperationLog log) {
        log.setLogId("LOG" + System.currentTimeMillis());
        logs.add(log);
        saveLogsToFile();
    }

    /**
     * 获取所有日志
     */
    public List<OperationLog> getAllLogs() {
        return new ArrayList<>(logs);
    }

    /**
     * 根据用户查询日志
     */
    public List<OperationLog> getLogsByUser(String userId) {
        return logs.stream()
                .filter(l -> l.getUserId().equals(userId))
                .collect(Collectors.toList());
    }

    // ==================== 数据持久化 ====================

    /**
     * 保存所有数据
     */
    public void saveData() {
        try {
            // 保存用户
            saveObject(users, DATA_DIR + "users.dat");
            // 保存商品
            saveObject(products, DATA_DIR + "products.dat");
            // 保存采购订单
            saveObject(purchaseOrders, DATA_DIR + "orders.dat");
            // 保存入库记录
            saveObject(stockInRecords, DATA_DIR + "stock_in.dat");
            // 保存出库记录
            saveObject(stockOutRecords, DATA_DIR + "stock_out.dat");
        } catch (IOException e) {
            System.err.println("保存数据失败: " + e.getMessage());
        }
    }

    /**
     * 加载所有数据
     */
    @SuppressWarnings("unchecked")
    public void loadData() {
        try {
            // 加载用户
            Object usersObj = loadObject(DATA_DIR + "users.dat");
            if (usersObj != null) {
                users = (Map<String, User>) usersObj;
            }

            // 加载商品
            Object productsObj = loadObject(DATA_DIR + "products.dat");
            if (productsObj != null) {
                products = (Map<String, Product>) productsObj;
            }

            // 加载采购订单
            Object ordersObj = loadObject(DATA_DIR + "orders.dat");
            if (ordersObj != null) {
                purchaseOrders = (Map<String, PurchaseOrder>) ordersObj;
            }

            // 加载入库记录
            Object stockInObj = loadObject(DATA_DIR + "stock_in.dat");
            if (stockInObj != null) {
                stockInRecords = Collections.synchronizedList((List<StockInRecord>) stockInObj);
            }

            // 加载出库记录
            Object stockOutObj = loadObject(DATA_DIR + "stock_out.dat");
            if (stockOutObj != null) {
                stockOutRecords = Collections.synchronizedList((List<StockOutRecord>) stockOutObj);
            }

            // 加载日志
            loadLogsFromFile();

            System.out.println("数据加载成功");
        } catch (Exception e) {
            System.err.println("加载数据失败: " + e.getMessage());
        }
    }

    /**
     * 保存日志到文件
     */
    private void saveLogsToFile() {
        try (PrintWriter writer = new PrintWriter(new FileWriter(DATA_DIR + "operation.log", true))) {
            if (!logs.isEmpty()) {
                OperationLog lastLog = logs.get(logs.size() - 1);
                writer.println(String.format("[%s] %s - %s - %s - %s",
                        lastLog.getOperationTime(),
                        lastLog.getUsername(),
                        lastLog.getModule(),
                        lastLog.getOperation(),
                        lastLog.getDetails()));
            }
        } catch (IOException e) {
            System.err.println("保存日志失败: " + e.getMessage());
        }
    }

    /**
     * 从文件加载日志
     */
    private void loadLogsFromFile() {
        File logFile = new File(DATA_DIR + "operation.log");
        if (logFile.exists()) {
            System.out.println("日志文件已加载");
        }
    }

    /**
     * 保存对象到文件
     */
    private void saveObject(Object obj, String filename) throws IOException {
        try (ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(filename))) {
            oos.writeObject(obj);
        }
    }

    /**
     * 从文件加载对象
     */
    private Object loadObject(String filename) {
        File file = new File(filename);
        if (!file.exists()) {
            return null;
        }

        try (ObjectInputStream ois = new ObjectInputStream(new FileInputStream(filename))) {
            return ois.readObject();
        } catch (Exception e) {
            System.err.println("加载文件失败 " + filename + ": " + e.getMessage());
            return null;
        }
    }

    /**
     * 数据备份
     */
    public boolean backup(String backupPath) {
        try {
            File backupDir = new File(backupPath);
            if (!backupDir.exists()) {
                backupDir.mkdirs();
            }

            // 复制所有数据文件
            File sourceDir = new File(DATA_DIR);
            if (sourceDir.exists()) {
                for (File file : sourceDir.listFiles()) {
                    copyFile(file, new File(backupPath, file.getName()));
                }
            }

            System.out.println("数据备份成功: " + backupPath);
            return true;
        } catch (Exception e) {
            System.err.println("数据备份失败: " + e.getMessage());
            return false;
        }
    }

    /**
     * 数据恢复
     */
    public boolean restore(String backupPath) {
        try {
            File backupDir = new File(backupPath);
            if (!backupDir.exists()) {
                return false;
            }

            // 复制备份文件到数据目录
            for (File file : backupDir.listFiles()) {
                copyFile(file, new File(DATA_DIR, file.getName()));
            }

            // 重新加载数据
            loadData();

            System.out.println("数据恢复成功");
            return true;
        } catch (Exception e) {
            System.err.println("数据恢复失败: " + e.getMessage());
            return false;
        }
    }

    /**
     * 复制文件
     */
    private void copyFile(File source, File dest) throws IOException {
        try (FileInputStream fis = new FileInputStream(source);
                FileOutputStream fos = new FileOutputStream(dest)) {
            byte[] buffer = new byte[4096];
            int bytesRead;
            while ((bytesRead = fis.read(buffer)) != -1) {
                fos.write(buffer, 0, bytesRead);
            }
        }
    }
}
