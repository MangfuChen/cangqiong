package com.sky.controller.admin;

import com.sky.dto.SetmealDTO;
import com.sky.dto.SetmealPageQueryDTO;
import com.sky.result.PageResult;
import com.sky.result.Result;
import com.sky.service.SetmealService;
import com.sky.vo.SetmealVO;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@Slf4j
@RequestMapping("/admin/setmeal")
@Api(value = "套餐接口")
@RequiredArgsConstructor
public class SetmealController {
    private final SetmealService setmealService;

    @PostMapping
    @ApiOperation("新增套餐")
    public Result saveSetmeal(@RequestBody SetmealDTO setmealDTO){

        return setmealService.saveSetmeal(setmealDTO);
    }

    @GetMapping("/page")
    @ApiOperation("分页查询")
    public Result<PageResult> page(SetmealPageQueryDTO setmealPageQueryDTO){
        PageResult pageResult = setmealService.pageQuery(setmealPageQueryDTO);
        return Result.success(pageResult);
    }

    @GetMapping("/{id}")
    @ApiOperation(value = "根据id查询套餐")
    public Result<SetmealVO> getByID(@PathVariable Long id){
        return setmealService.getByIdSetmeal(id);
    }

    @PutMapping
    @ApiOperation("套餐修改")
    public Result updateSetmeal(@RequestBody SetmealDTO setmealDTO){
        return setmealService.updateSetmeal(setmealDTO);
    }
    @PostMapping("/status/{status}")
    @ApiOperation("套餐启售或停售")
    public Result startOrStopStatus(@PathVariable Integer status,Long id){
        return setmealService.startOrStopStatus(status,id);
    }

    @DeleteMapping
    @ApiOperation("删除套餐")
    public Result deleteSetmeal(@RequestParam List<Long> ids){
        return setmealService.deleteSetmeal(ids);
    }
}
