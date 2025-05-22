package com.sky.service.impl;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.github.pagehelper.Page;
import com.github.pagehelper.PageHelper;
import com.sky.constant.MessageConstant;
import com.sky.context.BaseContext;
import com.sky.dto.*;
import com.sky.entity.*;
import com.sky.exception.AddressBookBusinessException;
import com.sky.exception.OrderBusinessException;
import com.sky.exception.ShoppingCartBusinessException;
import com.sky.mapper.*;
import com.sky.result.PageResult;
import com.sky.result.Result;
import com.sky.service.OrderService;
import com.sky.utils.HttpClientUtil;
import com.sky.vo.OrderPaymentVO;
import com.sky.vo.OrderStatisticsVO;
import com.sky.vo.OrderSubmitVO;
import com.sky.vo.OrderVO;
import com.sky.websocket.WebSocketServer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class OrderServiceImpl extends ServiceImpl<OrderMapper, Orders> implements OrderService {
    private final OrderMapper orderMapper;
    private final OrderDetailMapper orderDetailMapper;
    private final ShoppingCartMapper shoppingCartMapper;
    private final AddressBookMapper addressBookMapper;
    private final UserMapper userMapper;

    private final WebSocketServer webSocketServer;

    //地图工具属性注入
    @Value("${sky.shop.address}")
    private String shopAddress;//商家本地地址
    @Value("${sky.baidu.ak}")
    private String ak;//地图api使用



    @Override
    @Transactional
    public Result<OrderSubmitVO> submitOrder(OrdersSubmitDTO ordersSubmitDTO) {

    //处理业务异常
        //1.地址为空
        AddressBook addressBook = addressBookMapper.selectById(ordersSubmitDTO.getAddressBookId());
        if (addressBook==null){
            //抛出业务异常
            throw new AddressBookBusinessException(MessageConstant.ADDRESS_BOOK_IS_NULL);
        }
        //检查地址是否超过5000米
        checkOutOfRange(addressBook.getProvinceName()+addressBook.getCityName()+addressBook.getDistrictName()+addressBook.getDetail());

        //2.购物车为空
        Long currentUserId = BaseContext.getCurrentId();
        List<ShoppingCart> shoppingCarts = shoppingCartMapper.selectList(new LambdaQueryWrapper<ShoppingCart>()
                .eq(ShoppingCart::getUserId, currentUserId));
        if (shoppingCarts==null&&shoppingCarts.size()>0){
            throw new ShoppingCartBusinessException(MessageConstant.SHOPPING_CART_IS_NULL);
        }

    //插入数据
        //订单表
        Orders orders = new Orders();
        BeanUtils.copyProperties(ordersSubmitDTO, orders);
        orders.setOrderTime(LocalDateTime.now());
        orders.setPayStatus(Orders.UN_PAID);//未支付
        orders.setStatus(Orders.PENDING_PAYMENT);//待付款
        orders.setNumber(String.valueOf(System.currentTimeMillis())+currentUserId);//订单号
        orders.setPhone(addressBook.getPhone());//手机号
        orders.setConsignee(addressBook.getConsignee());//收货人
        orders.setUserId(currentUserId);
        orders.setAddress(addressBook.getDetail());

        orderMapper.insert(orders);
        List<OrderDetail> orderDetailList = new ArrayList();
        //订单详细表
        for (ShoppingCart shoppingCart : shoppingCarts) {
            OrderDetail orderDetail = new OrderDetail();
            BeanUtils.copyProperties(shoppingCart, orderDetail);
            orderDetail.setOrderId(orders.getId());
            orderDetailList.add(orderDetail);
        }
        orderDetailMapper.insertBatch(orderDetailList);

        shoppingCartMapper.delete(new LambdaQueryWrapper<ShoppingCart>().eq(ShoppingCart::getUserId, currentUserId));

        OrderSubmitVO orderSubmitVO = OrderSubmitVO.builder()
                .id(orders.getId())
                .orderTime(orders.getOrderTime())
                .orderNumber(orders.getNumber())
                .orderAmount(orders.getAmount())
                .build();
        return Result.success(orderSubmitVO);
    }

    /**
     * 订单支付
     *
     * @param ordersPaymentDTO
     * @return
     */
    public OrderPaymentVO payment(OrdersPaymentDTO ordersPaymentDTO) {
        // 当前登录用户id
        Long userId = BaseContext.getCurrentId();
        User user = userMapper.selectById(userId);

        //TODO 调用微信支付接口，生成预支付交易单
//        JSONObject jsonObject = weChatPayUtil.pay(
//                ordersPaymentDTO.getOrderNumber(), //商户订单号
//                new BigDecimal(0.01), //支付金额，单位 元
//                "苍穹外卖订单", //商品描述
//                user.getOpenid() //微信用户的openid
//        );

//        if (jsonObject.getString("code") != null && jsonObject.getString("code").equals("ORDERPAID")) {
//            throw new OrderBusinessException("该订单已支付");
//        }
//
//        OrderPaymentVO vo = jsonObject.toJavaObject(OrderPaymentVO.class);
//        vo.setPackageStr(jsonObject.getString("package"));
        //跳过支付，个人商户无法支付


        paySuccess(ordersPaymentDTO.getOrderNumber());
        return new OrderPaymentVO();
    }

    /**
     * 支付成功，修改订单状态
     *
     * @param outTradeNo
     */
    public void paySuccess(String outTradeNo) {

        // 根据订单号查询订单
        Orders ordersDB = orderMapper.selectOne(new LambdaQueryWrapper<Orders>().eq(

        Orders::getNumber, outTradeNo));

        // 根据订单id更新订单的状态、支付方式、支付状态、结账时间
        Orders orders = Orders.builder()
                .id(ordersDB.getId())
                .status(Orders.TO_BE_CONFIRMED)
                .payStatus(Orders.PAID)
                .checkoutTime(LocalDateTime.now())
                .build();

        orderMapper.updateById(orders);

        //通过websocket向客户端浏览器推送消息
        Map map = new HashMap();
        map.put("type", 1);//来单提醒
        map.put("orderId",ordersDB.getId().toString());
        map.put("content", "订单号"+outTradeNo);
        String jsonString = JSON.toJSONString(map);
        webSocketServer.sendToAllClient(jsonString);
    }

    @Override
    public PageResult pageHistoryOrders(OrdersPageQueryDTO ordersPageQueryDTO) {
        PageHelper.startPage(ordersPageQueryDTO.getPage(), ordersPageQueryDTO.getPageSize());
        //查询order
        Long currentUserId = BaseContext.getCurrentId();
        ordersPageQueryDTO.setUserId(currentUserId);
        Page<Orders> page =  orderMapper.pageHistoryOrders(ordersPageQueryDTO);

        // 查询出订单明细，并封装入OrderVO进行响应
        List<OrderVO> orderVOList = getOrderVOS(page);

        return new PageResult(page.getTotal(), orderVOList);
    }

    @Override
    public Result<OrderVO> getDetailByOrderId(Long orderId) {
        OrderVO orderVO = new OrderVO();
        //获取订单
        Orders orders = orderMapper.selectById(orderId);
        if (orders==null){
            throwOrderBussinessException();
        }
        BeanUtils.copyProperties(orders, orderVO);
        //获取订单详情
        List<OrderDetail> orderDetailList = getOrderDetailListByOrderId(orders.getId());
        orderVO.setOrderDetailList(orderDetailList);
        return Result.success(orderVO);
    }

    /**
     * - 待支付和待接单状态下，用户可直接取消订单
     * - 商家已接单状态下，用户取消订单需电话沟通商家
     * - 派送中状态下，用户取消订单需电话沟通商家
     * - 如果在待接单状态下取消订单，需要给用户退款
     * - 取消订单后需要将订单状态修改为“已取消”
     * @param orderId
     * @return
     */
    @Override
    public Result cancelByOrderId(Long orderId) {
        //查询订单出来，看看状态
        Orders ordersDB = orderMapper.selectById(orderId);
        //校验订单是否存在，安全冗余
        if (ordersDB==null){
            throwOrderBussinessException();
        }
        //订单状态 1待付款 2待接单 3已接单 4派送中 5已完成 6已取消
        if (ordersDB.getStatus()>2){
            //无法直接取消
            throw new OrderBusinessException(MessageConstant.ORDER_STATUS_ERROR);
        }

        // 订单处于待接单状态下取消，需要进行退款
        Orders orders = Orders.builder().id(orderId).userId(BaseContext.getCurrentId()).build();
        if (ordersDB.getStatus().equals(Orders.TO_BE_CONFIRMED)){
            //TODO 调用微信接口退款
//            weChatPayUtil.refund(
//                    ordersDB.getNumber(), //商户订单号
//                    ordersDB.getNumber(), //商户退款单号
//                    new BigDecimal(0.01),//退款金额，单位 元
//                    new BigDecimal(0.01));//原订单金额
            orders.setPayStatus(Orders.REFUND);
        }

        // 更新订单状态、取消原因、取消时间
        orders.setStatus(Orders.CANCELLED);
        orders.setCancelReason("用户取消");
        orders.setCancelTime(LocalDateTime.now());
        orderMapper.updateById(orders);
        return Result.success();
    }

    /**
     * 再来一单，将商品从新加入购物车中
     * @param orderId
     * @return
     */
    @Override
    public Result repetitionOrders(Long orderId) {
        //获取订单详情，详情中有商品
        List<OrderDetail> orderDetailList = getOrderDetailListByOrderId(orderId);

        List<ShoppingCart> shoppingCartList = orderDetailList.stream().map(x -> {
            ShoppingCart shoppingCart = new ShoppingCart();
            BeanUtils.copyProperties(x, shoppingCart);
            shoppingCart.setUserId(BaseContext.getCurrentId());
            return shoppingCart;
        }).collect(Collectors.toList());

        shoppingCartMapper.insertBatch(shoppingCartList);
        return Result.success();
    }

    private List<OrderDetail> getOrderDetailListByOrderId(Long orderId) {
        return orderDetailMapper.selectList(new LambdaQueryWrapper<OrderDetail>()
                .eq(OrderDetail::getOrderId, orderId));
    }

    @Override
    public PageResult conditionSearch(OrdersPageQueryDTO ordersPageQueryDTO) {
        PageHelper.startPage(ordersPageQueryDTO.getPage(), ordersPageQueryDTO.getPageSize());
        //查询order
        Page<Orders> page =  orderMapper.pageHistoryOrders(ordersPageQueryDTO);

        List<OrderVO> orderVOList = getOrderVOS(page);

        return new PageResult(page.getTotal(), orderVOList);
    }

    @Override
    public Result<OrderStatisticsVO> statistics() {
        // 根据状态，分别查询出待接单、待派送、派送中的订单数量
        LambdaQueryWrapper<Orders> lambdaQueryWrapper = new LambdaQueryWrapper<>();
        lambdaQueryWrapper.eq(Orders::getStatus, Orders.TO_BE_CONFIRMED);
        //待接单
        Integer toBeConfirmed = Math.toIntExact(orderMapper.selectCount(lambdaQueryWrapper));
        //待派送
        lambdaQueryWrapper.clear();
        lambdaQueryWrapper.eq(Orders::getStatus, Orders.CONFIRMED);
        Integer confirmed =Math.toIntExact(orderMapper.selectCount(lambdaQueryWrapper));
        //派送中
        lambdaQueryWrapper.clear();
        lambdaQueryWrapper.eq(Orders::getStatus, Orders.DELIVERY_IN_PROGRESS);
        Integer deliveryInProgress=Math.toIntExact(orderMapper.selectCount(lambdaQueryWrapper));

        //封装
        OrderStatisticsVO orderStatisticsVO = new OrderStatisticsVO();
        orderStatisticsVO.setConfirmed(confirmed);
        orderStatisticsVO.setToBeConfirmed(toBeConfirmed);
        orderStatisticsVO.setDeliveryInProgress(deliveryInProgress);
        return Result.success(orderStatisticsVO);
    }

    /**
     * 商家接单
     * @param ordersConfirmDTO
     * @return
     */
    @Override
    public Result confirmOrder(OrdersConfirmDTO ordersConfirmDTO) {
        Orders orders = Orders.builder().id(ordersConfirmDTO.getId())
                .status(Orders.CONFIRMED).build();
        orderMapper.updateById(orders);
        return Result.success();
    }

    /**
     * 商家拒单
     * @param rejectionDTO
     * @return
     */
    @Override
    public Result rejectionOrder(OrdersRejectionDTO rejectionDTO) {
        //需要订单状态为2才可以拒单
        Orders ordersDB = orderMapper.selectById(rejectionDTO.getId());
        if (!ordersDB.getStatus().equals(Orders.TO_BE_CONFIRMED)||ordersDB==null){
            throw new OrderBusinessException(MessageConstant.ORDER_STATUS_ERROR);
        }

    //进行拒单流程
        //TODO 已付款，需要进行微信退款
        Integer payStatus = ordersDB.getStatus();
        //if (payStatus == Orders.PAID) {
            //用户已支付，需要退款
//            String refund = weChatPayUtil.refund(
//                    ordersDB.getNumber(),
//                    ordersDB.getNumber(),
//                    new BigDecimal(0.01),
//                    new BigDecimal(0.01));
//            log.info("申请退款：{}", refund);
        //}
        //数据库进行修改
        Orders orders = Orders.builder()
                .id(rejectionDTO.getId())
                .rejectionReason(rejectionDTO.getRejectionReason())
                .status(Orders.CANCELLED)
                .cancelReason(rejectionDTO.getRejectionReason())
                .cancelTime(LocalDateTime.now())
                .build();
        int updateNum = orderMapper.updateById(orders);
        if (updateNum>0){
            return Result.success();
        }
        return Result.error("已接单");
    }

    /**
     * 商家取消订单
     * @param ordersCancelDTO
     * @return
     */
    @Override
    public Result cancelOrder(OrdersCancelDTO ordersCancelDTO) {
        // 根据id查询订单
        Orders ordersDB = orderMapper.selectById(ordersCancelDTO.getId());
        //安全冗余
        if (ordersDB==null){
            throwOrderBussinessException();
        }
        Integer payStatus = ordersDB.getPayStatus();
        if (payStatus.equals(Orders.CANCELLED)){
            return Result.error("订单已取消");
        }

        //TODO 需要进行微信退款

//        if (payStatus == 1) {
//            //用户已支付，需要退款
//            String refund = weChatPayUtil.refund(
//                    ordersDB.getNumber(),
//                    ordersDB.getNumber(),
//                    new BigDecimal(0.01),
//                    new BigDecimal(0.01));
//            log.info("申请退款：{}", refund);
//        }
        //修改订单状态
        Orders orders = Orders.builder()
                .id(ordersCancelDTO.getId())
                .status(Orders.CANCELLED)
                .cancelReason(ordersCancelDTO.getCancelReason())
                .build();
        int updateNum = orderMapper.updateById(orders);
        if (updateNum>0){
            return Result.success();
        }
        return Result.error("订单已取消");
    }

    /**
     * 商家派送订单
     * @param orderId   订单id
     * @return  返回Resule结果
     */
    @Override
    public Result deliveryOrder(Long orderId) {
        //安全冗余
        Orders ordersDB = orderMapper.selectById(orderId);
        Integer statusDB = ordersDB.getStatus();
        if (ordersDB==null||!statusDB.equals(Orders.CONFIRMED)){
            throwOrderBussinessException();
        }
        Orders orders = Orders.builder().id(orderId)
                .status(Orders.DELIVERY_IN_PROGRESS)
                .build();
        int updateNum = orderMapper.updateById(orders);
        if (updateNum>0){
            return Result.success();
        }
        return Result.error("订单在派送中");
    }

    /**
     * 商家完成订单
     * @param orderId   订单id
     * @return  集合结果
     */
    @Override
    public Result completeOrder(Long orderId) {
        //安全冗余
        Orders ordersDB = orderMapper.selectById(orderId);
        Integer statusDB = ordersDB.getStatus();
        if (ordersDB==null||!statusDB.equals(Orders.DELIVERY_IN_PROGRESS)){
            throwOrderBussinessException();
        }
        //修改数据库状态

        Orders orders = Orders.builder()
                .id(orderId)
                .status(Orders.COMPLETED)
                .build();
        int updateNum = orderMapper.updateById(orders);
        if (updateNum>0){
            return Result.success();
        }
        return Result.error("订单已完成");
    }

    @Override
    public Result reminderOrder(Long orderId) {
        Orders ordersDB = orderMapper.selectById(orderId);
        if (ordersDB==null){
            throwOrderBussinessException();
        }

        Map map = new HashMap();
        map.put("type", 2);//来单提醒
        map.put("orderId",ordersDB.getId().toString());
        map.put("content", "订单号"+ordersDB.getNumber());
        String jsonString = JSON.toJSONString(map);
        webSocketServer.sendToAllClient(jsonString);
        return Result.success();
    }

    private void throwOrderBussinessException() {
        throw new OrderBusinessException(MessageConstant.ORDER_NOT_FOUND);
    }

    /**
     * 检查客户的收货地址是否超出配送范围
     * @param address 用户地址位置
     */
    private void checkOutOfRange(String address) {
        //拼接参数
        Map map = new HashMap();
        map.put("address",shopAddress);
        map.put("output","json");
        map.put("ak",ak);

        //获取店铺的经纬度坐标
        String shopCoordinate = HttpClientUtil.doGet("https://api.map.baidu.com/geocoding/v3", map);

        //百度地图返回的数据，进行解析,返回状态为0代表	服务请求正常召回
        JSONObject jsonObject = JSON.parseObject(shopCoordinate);
        if(!jsonObject.getString("status").equals("0")){
            log.debug("商家地址解析失败");
            throw new OrderBusinessException("店铺地址解析失败");
        }

    //商家地址数据解析
        //获取商家地址的经纬度
        JSONObject location = jsonObject.getJSONObject("result").getJSONObject("location");
        //	lat	float	纬度值
        String lat = location.getString("lat");
        //lng	float	经度值
        String lng = location.getString("lng");
        //店铺经纬度坐标
        String shopLngLat = lat + "," + lng;
        //用户地址位置添加进入
        map.put("address",address);

        //获取用户收货地址的经纬度坐标
        String userCoordinate = HttpClientUtil.doGet("https://api.map.baidu.com/geocoding/v3", map);
        jsonObject = JSON.parseObject(userCoordinate);
        //百度地图返回的数据，进行解析,返回状态为0代表	服务请求正常召回
        if(!jsonObject.getString("status").equals("0")){
            log.debug("用户地址解析失败");
            throw new OrderBusinessException("收货地址解析失败");
        }

    //用户地址数据解析
        location = jsonObject.getJSONObject("result").getJSONObject("location");
        //	lat	float	纬度值
        lat = location.getString("lat");
        //lng	float	经度值
        lng = location.getString("lng");
        //用户收货地址经纬度坐标
        String userLngLat = lat + "," + lng;

    //路线规划
        //origin 起点	double,double 起点经纬度，格式为：纬度,经度；小数点后不超过6位
        map.put("origin",shopLngLat);
        //destination	终点	double,double		终点经纬度，格式为：纬度,经度；小数点后不超过6位，
        map.put("destination",userLngLat);
        //steps_info	是否下发step详情 0：不下发step详情
        map.put("steps_info","0");

        //v1Api路线规划
        String json = HttpClientUtil.doGet("https://api.map.baidu.com/directionlite/v1/driving", map);

        jsonObject = JSON.parseObject(json);
        if(!jsonObject.getString("status").equals("0")){
            throw new OrderBusinessException("配送路线规划失败");
        }

        //数据解析
        JSONObject result = jsonObject.getJSONObject("result");
        JSONArray jsonArray = (JSONArray) result.get("routes");
        Integer distance = (Integer) ((JSONObject) jsonArray.get(0)).get("distance");

        if(distance > 5000){
            //配送距离超过5000米
            throw new OrderBusinessException("超出配送范围");
        }
    }


    /**
     *  根据page封装进orderVo
     */
    private List<OrderVO> getOrderVOS(Page<Orders> page) {
        // 查询出订单明细，并封装入OrderVO进行响应
        List<OrderVO> orderVOList = new ArrayList();

        if (page !=null&& page.size()>0){
            for (Orders orders : page) {
                List<OrderDetail> orderDetailList = getOrderDetailListByOrderId(orders.getId());
                //构造VO
                OrderVO orderVO = new OrderVO();
                BeanUtils.copyProperties(orders, orderVO);
                orderVO.setOrderDetailList(orderDetailList);
                //订单菜品信息
                String orderDishes = getOrderDishesStr(orders);
                orderVO.setOrderDishes(orderDishes);
                orderVOList.add(orderVO);
            }
        }
        return orderVOList;
    }

    /**
     * 根据订单id获取菜品信息字符串
     *
     * @param orders
     * @return
     */
    private String getOrderDishesStr(Orders orders) {
        // 查询订单菜品详情信息（订单中的菜品和数量）
        List<OrderDetail> orderDetailList = getOrderDetailListByOrderId(orders.getId());
        // 将每一条订单菜品信息拼接为字符串（格式：宫保鸡丁*3；）
        List<String> orderDishList = orderDetailList.stream().map(x -> {
            String orderDish = x.getName() + "*" + x.getNumber() + ";";
            return orderDish;
        }).collect(Collectors.toList());
        // 将该订单对应的所有菜品信息拼接在一起
        return String.join("", orderDishList);
    }
}
