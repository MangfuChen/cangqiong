package com.sky.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.sky.result.Result;
import com.sky.vo.OrderReportVO;
import com.sky.vo.SalesTop10ReportVO;
import com.sky.vo.TurnoverReportVO;
import com.sky.vo.UserReportVO;

import javax.servlet.http.HttpServletResponse;
import java.time.LocalDate;

public interface ReportService  {
	Result<TurnoverReportVO> turnoverStatistics(LocalDate begin, LocalDate end);

	Result<UserReportVO> userStatistics(LocalDate begin, LocalDate end);

	/**
	 * 统计指定时间区间内的订单数据
	 * @param begin
	 * @param end
	 * @return
	 */
	Result<OrderReportVO> ordersStatistics(LocalDate begin, LocalDate end);

	Result<SalesTop10ReportVO> getSalesTop10(LocalDate begin, LocalDate end);

	/**
	 * 导出运营数据报表
	 * @param response
	 */
	void exportBusinessData(HttpServletResponse response);
}
