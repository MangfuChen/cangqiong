package com.sky.controller.admin;

import com.sky.dto.OrdersCancelDTO;
import com.sky.dto.OrdersConfirmDTO;
import com.sky.dto.OrdersPageQueryDTO;
import com.sky.dto.OrdersRejectionDTO;
import com.sky.result.PageResult;
import com.sky.result.Result;
import com.sky.service.OrderService;
import com.sky.vo.OrderStatisticsVO;
import com.sky.vo.OrderVO;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

@RestController("adminOrderController")
@RequestMapping("/admin/order")
@Api(tags = "管理端端-订单相关接口")
@Slf4j
@RequiredArgsConstructor
public class OrderController {
	private final OrderService orderService;

	@GetMapping("/conditionSearch")
	@ApiOperation("订单搜索")
	public Result<PageResult> conditionSearch(OrdersPageQueryDTO ordersPageQueryDTO){
		PageResult pageResult = orderService.conditionSearch(ordersPageQueryDTO);
		return Result.success(pageResult);
	}

	@GetMapping("/statistics")
	@ApiOperation("各个状态的订单数量统计")
	public Result<OrderStatisticsVO> statistics(){
		return orderService.statistics();
	}

	@GetMapping("/details/{orderId}")
	@ApiOperation("查询订单详情")
	public Result<OrderVO> getOrderDetailsByOrderId(@PathVariable("orderId") Long orderId) {
		return orderService.getDetailByOrderId(orderId);
	}

	/**
	 * 商家接单
	 * @param ordersConfirmDTO
	 * @return
	 */
	@PutMapping("/confirm")
	@ApiOperation("商家接单")
	public Result confirmOrder(@RequestBody OrdersConfirmDTO ordersConfirmDTO){
		return orderService.confirmOrder(ordersConfirmDTO);
	}

	@PutMapping("/rejection")
	@ApiOperation("商家拒单")
	public Result rejectionOrder(@RequestBody OrdersRejectionDTO rejectionDTO){
		return orderService.rejectionOrder(rejectionDTO);
	}

	@PutMapping("/cancel")
	@ApiOperation("商家取消订单")
	public Result cancelOrder(@RequestBody OrdersCancelDTO ordersCancelDTO){
		return orderService.cancelOrder(ordersCancelDTO);
	}

	@PutMapping("/delivery/{orderId}")
	@ApiOperation("商家选择派送")
	public Result deliveryOrder(@PathVariable("orderId") Long orderId){
		return orderService.deliveryOrder(orderId);
	}

	@PutMapping("/complete/{orderId}")
	@ApiOperation("商家完成订单")
	public Result completeOrder(@PathVariable("orderId") Long orderId){
		return orderService.completeOrder(orderId);
	}

}
