package com.sky.controller.user;

import com.sky.dto.OrdersPageQueryDTO;
import com.sky.dto.OrdersPaymentDTO;
import com.sky.dto.OrdersSubmitDTO;
import com.sky.result.PageResult;
import com.sky.result.Result;
import com.sky.service.OrderService;
import com.sky.vo.OrderPaymentVO;
import com.sky.vo.OrderSubmitVO;
import com.sky.vo.OrderVO;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController("userOrderController")
@RequestMapping("/user/order")
@Api(tags = "C端-订单相关接口")
@Slf4j
@RequiredArgsConstructor
public class OrderController {
    private final OrderService orderService;

    @PostMapping("/submit")
    @ApiOperation("用户下单")
    public Result<OrderSubmitVO> submit(@RequestBody OrdersSubmitDTO ordersSubmitDTO){
        return orderService.submitOrder(ordersSubmitDTO);
    }

    /**
     * 订单支付
     *
     * @param ordersPaymentDTO
     * @return
     */
    @PutMapping("/payment")
    @ApiOperation("订单支付")
    public Result<OrderPaymentVO> payment(@RequestBody OrdersPaymentDTO ordersPaymentDTO) throws Exception {
        log.info("订单支付：{}", ordersPaymentDTO);
        OrderPaymentVO orderPaymentVO = orderService.payment(ordersPaymentDTO);
        log.info("生成预支付交易单：{}", orderPaymentVO);
        return Result.success(orderPaymentVO);
    }

    /**
     * 查看历史订单
     * @param ordersPageQueryDTO
     * @return
     */
    @GetMapping("/historyOrders")
    @ApiOperation("查看历史订单")
    public Result<PageResult> pageHistoryOrders(OrdersPageQueryDTO ordersPageQueryDTO){
        PageResult pageResult = orderService.pageHistoryOrders(ordersPageQueryDTO);
        return Result.success(pageResult);
    }

    /**
     * 查询订单详情
     * @param orderId
     * @return
     */
    @GetMapping("/orderDetail/{orderId}")
    @ApiOperation("查询订单详情")
    public Result<OrderVO> getDetailByOrderId(@PathVariable Long orderId){
        return orderService.getDetailByOrderId(orderId);
    }

    @PutMapping("/cancel/{orderId}")
    @ApiOperation("取消订单")
    public Result cancelByOrderId(@PathVariable Long orderId){
        return orderService.cancelByOrderId(orderId);
    }

    @PostMapping("/repetition/{orderId}")
    @ApiOperation("再来一单")
    public Result repetitionOrders(@PathVariable Long orderId){
        return orderService.repetitionOrders(orderId);
    }

    @GetMapping("/reminder/{orderId}")
    @ApiOperation("用户催单")
    public Result reminderOrder(@PathVariable("orderId") Long orderId){
        return orderService.reminderOrder(orderId);
    }
}
