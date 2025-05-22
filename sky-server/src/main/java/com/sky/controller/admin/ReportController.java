package com.sky.controller.admin;

import com.sky.result.Result;
import com.sky.service.ReportService;
import com.sky.vo.OrderReportVO;
import com.sky.vo.SalesTop10ReportVO;
import com.sky.vo.TurnoverReportVO;
import com.sky.vo.UserReportVO;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletResponse;
import java.time.LocalDate;

@RestController("adminReportController")
@RequestMapping("/admin/report")
@Api(tags = "管理端端-数据展示相关接口")
@Slf4j
@RequiredArgsConstructor
public class ReportController {
	private final ReportService reportService;
	@GetMapping("/turnoverStatistics")
	@ApiOperation("营业额统计")
	public Result<TurnoverReportVO> turnoverStatistics(
		@DateTimeFormat(pattern = "yyyy-MM-dd" )LocalDate begin
		,@DateTimeFormat(pattern = "yyyy-MM-dd" )LocalDate end
	){
		return reportService.turnoverStatistics(begin,end);
	}

	/**
	 * 用户统计
	 * @return
	 */
	@GetMapping("/userStatistics")
	@ApiOperation("用户统计")
	public Result<UserReportVO> userStatistics(
		@DateTimeFormat(pattern = "yyyy-MM-dd" )LocalDate begin
		,@DateTimeFormat(pattern = "yyyy-MM-dd" )LocalDate end
	){
		return reportService.userStatistics(begin,end);
	}
	@GetMapping("/ordersStatistics")
	@ApiOperation("订单统计")
	public Result<OrderReportVO> ordersStatistics(
			@DateTimeFormat(pattern = "yyyy-MM-dd" )LocalDate begin
			,@DateTimeFormat(pattern = "yyyy-MM-dd" )LocalDate end
	){
		return reportService.ordersStatistics(begin,end);
	}


	@GetMapping("/top10")
	@ApiOperation("销量前10")
	public Result<SalesTop10ReportVO> top10(
			@DateTimeFormat(pattern = "yyyy-MM-dd" )LocalDate begin
			,@DateTimeFormat(pattern = "yyyy-MM-dd" )LocalDate end
	){
		return reportService.getSalesTop10(begin,end);
	}

	@GetMapping("/export")
	@ApiOperation("导出运营数据报表-excel形式")
	public void export(HttpServletResponse response){
		reportService.exportBusinessData(response);
	}
}
