package com.sky.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.sky.constant.RedisConstant;
import com.sky.dto.GoodsSalesDTO;
import com.sky.entity.Orders;
import com.sky.mapper.OrderMapper;
import com.sky.mapper.UserMapper;
import com.sky.result.Result;
import com.sky.service.ReportService;
import com.sky.service.WorkspaceService;
import com.sky.vo.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang.StringUtils;
import org.apache.poi.xssf.usermodel.XSSFRow;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletResponse;
import javax.xml.datatype.Duration;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Service
@Slf4j
@RequiredArgsConstructor
public class ReportServiceImpl implements ReportService {

	private final OrderMapper orderMapper;
	private final RedisTemplate redisTemplate;
	private final UserMapper userMapper;

	private final WorkspaceService workspaceService;
	/**
	 * 指定日期区间内的营业额统计
	 * @param begin
	 * @param end
	 * @return
	 */
	@Override
	public Result<TurnoverReportVO> turnoverStatistics(LocalDate begin, LocalDate end) {
		//先从redis中获取数据
		String redisKey = RedisConstant.REPORT_AMOUNT + begin + "::" + end;
		TurnoverReportVO redisVO = (TurnoverReportVO) redisTemplate.opsForValue().get(redisKey);
		if (redisVO!=null){
			return Result.success(redisVO);
		}

		//封装日期
		List<Double> turnoverList = new ArrayList<>();
		List<LocalDate> dateList = getDateList(begin, end);

		String dateJson = StringUtils.join(dateList, ",");



		//查询日期数据
		LambdaQueryWrapper<Orders> ordersLambdaQueryWrapper = new LambdaQueryWrapper<>();

		for (LocalDate localDate : dateList) {
			//查询date日期对应的营业额数据
			LocalDateTime dayBeginTime = LocalDateTime.of(localDate, LocalTime.MIN);
			LocalDateTime dayEndTime = LocalDateTime.of(localDate, LocalTime.MAX);
			Map map = new HashMap<>();
			map.put("begin", dayBeginTime);
			map.put("end", dayEndTime);
			map.put("status", Orders.COMPLETED);
			Double turnover = orderMapper.sumByMap(map);
			turnover=turnover==null?0.0:turnover;
			turnoverList.add(turnover);
		}
		String amountJson = StringUtils.join(turnoverList, ",");
		//封装数据
		TurnoverReportVO vo = TurnoverReportVO.builder()
				.dateList(dateJson)
				.turnoverList(amountJson)
				.build();
		redisTemplate.opsForValue().set(redisKey, vo, 12, TimeUnit.HOURS);
		return Result.success(vo);
	}

	@Override
	public Result<UserReportVO> userStatistics(LocalDate begin, LocalDate end) {
		//先从redis中获取数据
		String redisKey = RedisConstant.REPORT_USER + begin + "::" + end;
		UserReportVO redisVO = (UserReportVO) redisTemplate.opsForValue().get(redisKey);
		if (redisVO!=null){
			return Result.success(redisVO);
		}

		//存放从begin到end之间的每天对应的日期
		List<LocalDate> dateList = getDateList(begin, end);

		//存放每天的新增用户数量 select count(id) from user where create_time < ? and create_time > ?
		List<Integer> newUserList = new ArrayList<>();
		//存放每天的总用户数量 select count(id) from user where create_time < ?
		List<Integer> totalUserList = new ArrayList<>();

		for (LocalDate date : dateList) {
			LocalDateTime beginTime = LocalDateTime.of(date, LocalTime.MIN);
			LocalDateTime endTime = LocalDateTime.of(date, LocalTime.MAX);

			Map map = new HashMap();
			map.put("end", endTime);

			//总用户数量
			Integer totalUser = userMapper.countByMap(map);

			map.put("begin", beginTime);
			//新增用户数量
			Integer newUser = userMapper.countByMap(map);

			totalUserList.add(totalUser);
			newUserList.add(newUser);
		}

		//封装结果数据
		UserReportVO vo = UserReportVO
				.builder()
				.dateList(StringUtils.join(dateList, ","))
				.totalUserList(StringUtils.join(totalUserList, ","))
				.newUserList(StringUtils.join(newUserList, ","))
				.build();

		redisTemplate.opsForValue().set(redisKey, vo, 12, TimeUnit.HOURS);
		return Result.success(vo);

	}

	private List<LocalDate> getDateList(LocalDate begin, LocalDate end) {
		List<LocalDate> dateList = new ArrayList<>();

		dateList.add(begin);

		while (!begin.equals(end)) {
			begin = begin.plusDays(1);
			dateList.add(begin);
		}
		return dateList;
	}

	/**
	 *
	 * @param begin
	 * @param end
	 * @return
	 */
	@Override
	public Result<OrderReportVO> ordersStatistics(LocalDate begin, LocalDate end) {
		//获取datelist
		List<LocalDate> dateList = getDateList(begin, end);
		//遍历日期，查询每天有效订单总数，订单总数

		//存放每天的订单总数
		List<Integer> orderCountList = new ArrayList<>();
		//存放每天的有效订单数
		List<Integer> validOrderCountList = new ArrayList<>();
		for (LocalDate date : dateList) {
			LocalDateTime beginTime = LocalDateTime.of(date, LocalTime.MIN);
			LocalDateTime endTime = LocalDateTime.of(date, LocalTime.MAX);
			Integer orderCount = getOrderCount(beginTime, endTime, null);

			//查询每天的有效订单数 select count(id) from orders where order_time > ? and order_time < ? and status = 5
			Integer validOrderCount = getOrderCount(beginTime, endTime, Orders.COMPLETED);

			orderCountList.add(orderCount);
			validOrderCountList.add(validOrderCount);
		}
		//总量
		Integer totalOrderCount = orderCountList.stream().reduce(Integer::sum).get();
		//有效总量
		Integer totalValidOrderCount = validOrderCountList.stream().reduce(Integer::sum).get();


		Double orderCompletionRate = 0.0;
		if (totalOrderCount!=0){
			orderCompletionRate=totalValidOrderCount.doubleValue()/totalOrderCount;
		}

		OrderReportVO vo = OrderReportVO.builder()
				.dateList(StringUtils.join(dateList, ","))
				.orderCountList(StringUtils.join(orderCountList, ","))
				.validOrderCountList(StringUtils.join(validOrderCountList, ","))
				.totalOrderCount(totalOrderCount)
				.validOrderCount(totalValidOrderCount)
				.orderCompletionRate(orderCompletionRate)
				.build();
		return Result.success(vo);
	}


	/**
	 * 根据条件统计订单数量
	 * @param begin
	 * @param end
	 * @param status
	 * @return
	 */
	private Integer getOrderCount(LocalDateTime begin, LocalDateTime end, Integer status){
		Map map = new HashMap();
		map.put("begin",begin);
		map.put("end",end);
		map.put("status",status);

		return orderMapper.countByMap(map);
	}


	/**
	 * 统计指定时间区间内的销量排名前10
	 * @param begin
	 * @param end
	 * @return
	 */
	public Result<SalesTop10ReportVO> getSalesTop10(LocalDate begin, LocalDate end) {
		//先从redis中获取数据
		String redisKey = RedisConstant.REPORT_SALES + begin + "::" + end;
		SalesTop10ReportVO redisVO = (SalesTop10ReportVO) redisTemplate.opsForValue().get(redisKey);
		if (redisVO!=null){
			return Result.success(redisVO);
		}



		LocalDateTime beginTime = LocalDateTime.of(begin, LocalTime.MIN);
		LocalDateTime endTime = LocalDateTime.of(end, LocalTime.MAX);

		List<GoodsSalesDTO> salesTop10 = orderMapper.getSalesTop10(beginTime, endTime);
		List<String> names = salesTop10.stream().map(GoodsSalesDTO::getName).collect(Collectors.toList());
		String nameList = StringUtils.join(names, ",");

		List<Integer> numbers = salesTop10.stream().map(GoodsSalesDTO::getNumber).collect(Collectors.toList());
		String numberList = StringUtils.join(numbers, ",");

		//封装返回结果数据
		SalesTop10ReportVO vo = SalesTop10ReportVO
				.builder()
				.nameList(nameList)
				.numberList(numberList)
				.build();
		redisTemplate.opsForValue().set(redisKey, vo, 12, TimeUnit.HOURS);
		return Result.success(vo);
	}

	@Override
	public void exportBusinessData(HttpServletResponse response) {
		//1.查询数据库，获取营业数据  最近30天
		LocalDate dateBegin = LocalDate.now().minusDays(30);
		LocalDate dateEnd = LocalDate.now().minusDays(1);
		LocalDateTime begin = LocalDateTime.of(dateBegin, LocalTime.MIN);
		LocalDateTime end = LocalDateTime.of(dateEnd, LocalTime.MAX);

		BusinessDataVO businessData = workspaceService.getBusinessData(begin, end);

		//2.通过poi将数据写入excel中
		InputStream inputStream = this.getClass().getClassLoader().getResourceAsStream("template/运营数据报表模板.xlsx");

		try (XSSFWorkbook excel = new XSSFWorkbook(inputStream);ServletOutputStream outputStream = response.getOutputStream();){
			//填充数据
			XSSFSheet sheet = excel.getSheet("sheet1");
		 	sheet.getRow(1).getCell(1).setCellValue("时间"+dateBegin+"——"+dateEnd);
			XSSFRow row = sheet.getRow(3);
			row.getCell(2).setCellValue(businessData.getTurnover());
			row.getCell(4).setCellValue(businessData.getOrderCompletionRate());
			row.getCell(6).setCellValue(businessData.getNewUsers());

			//获得第5行
			row = sheet.getRow(4);
			row.getCell(2).setCellValue(businessData.getValidOrderCount());
			row.getCell(4).setCellValue(businessData.getUnitPrice());


			//填充明细数据
			for (int i = 0; i < 30; i++) {
				LocalDate localDate = dateBegin.plusDays(i);
				//查询某一天数据
				BusinessDataVO businessDataDB = workspaceService.getBusinessData(LocalDateTime.of(localDate, LocalTime.MIN), LocalDateTime.of(localDate, LocalTime.MAX));
				row = sheet.getRow(7+i);
				row.getCell(1).setCellValue(localDate.toString());
				row.getCell(2).setCellValue(businessDataDB.getTurnover());
				row.getCell(3).setCellValue(businessDataDB.getValidOrderCount());
				row.getCell(4).setCellValue(businessDataDB.getOrderCompletionRate());
				row.getCell(5).setCellValue(businessDataDB.getUnitPrice());
				row.getCell(6).setCellValue(businessDataDB.getNewUsers());
			}



			//3.通过输出流将excel文件下载到客户端浏览器

			excel.write(outputStream);
		} catch (IOException e) {
			throw new RuntimeException(e);
		}finally {

		}


	}
}
