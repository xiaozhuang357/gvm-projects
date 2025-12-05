package com._404.wms.dao;

import com._404.wms.model.PurchaseOrder;
import com._404.wms.model.PurchaseOrder.OrderStatus;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 采购订单数据访问接口
 */
public interface PurchaseOrderDao extends BaseDao<PurchaseOrder, String> {

    /**
     * 根据采购员ID查询订单
     * 
     * @param purchaserId 采购员ID
     * @return 订单列表
     */
    List<PurchaseOrder> findByPurchaserId(String purchaserId);

    /**
     * 根据状态查询订单
     * 
     * @param status 订单状态
     * @return 订单列表
     */
    List<PurchaseOrder> findByStatus(OrderStatus status);

    /**
     * 根据多个状态查询订单
     * 
     * @param statuses 订单状态列表
     * @return 订单列表
     */
    List<PurchaseOrder> findByStatuses(List<OrderStatus> statuses);

    /**
     * 根据供应商查询订单
     * 
     * @param supplier 供应商名称
     * @return 订单列表
     */
    List<PurchaseOrder> findBySupplier(String supplier);

    /**
     * 根据创建时间范围查询订单
     * 
     * @param startTime 开始时间
     * @param endTime   结束时间
     * @return 订单列表
     */
    List<PurchaseOrder> findByCreateTimeBetween(LocalDateTime startTime, LocalDateTime endTime);

    /**
     * 根据预计交货日期范围查询订单
     * 
     * @param startDate 开始日期
     * @param endDate   结束日期
     * @return 订单列表
     */
    List<PurchaseOrder> findByExpectedDeliveryDateBetween(LocalDate startDate, LocalDate endDate);

    /**
     * 查询待审批订单（待部门经理或总经理审批）
     * 
     * @return 待审批订单列表
     */
    List<PurchaseOrder> findPendingApproval();

    /**
     * 更新订单状态
     * 
     * @param orderId 订单ID
     * @param status  新状态
     * @return 更新成功返回true
     */
    boolean updateStatus(String orderId, OrderStatus status);

    /**
     * 审批订单
     * 
     * @param orderId      订单ID
     * @param approverId   审批人ID
     * @param approverName 审批人姓名
     * @param status       审批后的状态
     * @param comment      审批意见
     * @return 审批成功返回true
     */
    boolean approve(String orderId, String approverId, String approverName,
            OrderStatus status, String comment);

    /**
     * 设置到货时间
     * 
     * @param orderId     订单ID
     * @param arrivalTime 到货时间
     * @return 更新成功返回true
     */
    boolean setArrivalTime(String orderId, LocalDateTime arrivalTime);

    /**
     * 统计各状态订单数量
     * 
     * @return 状态-数量映射
     */
    java.util.Map<OrderStatus, Long> countByStatus();
}
