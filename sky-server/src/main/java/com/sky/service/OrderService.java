package com.sky.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.sky.dto.*;
import com.sky.entity.Orders;
import com.sky.result.PageResult;
import com.sky.result.Result;
import com.sky.vo.OrderPaymentVO;
import com.sky.vo.OrderStatisticsVO;
import com.sky.vo.OrderSubmitVO;
import com.sky.vo.OrderVO;

public interface OrderService extends IService<Orders> {
    Result<OrderSubmitVO> submitOrder(OrdersSubmitDTO ordersSubmitDTO);

	OrderPaymentVO payment(OrdersPaymentDTO ordersPaymentDTO);

	void paySuccess(String outTradeNo);

	PageResult pageHistoryOrders(OrdersPageQueryDTO ordersPageQueryDTO);

	Result<OrderVO> getDetailByOrderId(Long orderId);

	Result cancelByOrderId(Long orderId);

	Result repetitionOrders(Long orderId);

	PageResult conditionSearch(OrdersPageQueryDTO ordersPageQueryDTO);

	Result<OrderStatisticsVO> statistics();

	Result confirmOrder(OrdersConfirmDTO ordersConfirmDTO);

	Result rejectionOrder(OrdersRejectionDTO rejectionDTO);

	Result cancelOrder(OrdersCancelDTO ordersCancelDTO);

	Result deliveryOrder(Long orderId);

	Result completeOrder(Long orderId);

	Result reminderOrder(Long orderId);
}
