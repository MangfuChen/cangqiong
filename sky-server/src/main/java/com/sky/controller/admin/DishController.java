package com.sky.controller.admin;

import com.sky.dto.DishDTO;
import com.sky.dto.DishPageQueryDTO;
import com.sky.entity.Dish;
import com.sky.mapper.DishMapper;
import com.sky.result.PageResult;
import com.sky.result.Result;
import com.sky.service.DishService;
import com.sky.vo.DishVO;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@Slf4j
@RequestMapping("/admin/dish")
@Api(value = "菜品接口")
@RequiredArgsConstructor
public class DishController {

    private final DishService dishService;
    /**
     * 新增菜品
     * @param dishDTO
     * @return
     */
    @PostMapping
    @ApiOperation(value = "新增菜品")
    public Result save(@RequestBody DishDTO dishDTO){

        return dishService.saveWithFlavor(dishDTO);
    }

    @GetMapping("/page")
    @ApiOperation(value = "菜品查询")
    public Result<PageResult> page(DishPageQueryDTO dishPageQueryDTO){
        log.info("菜品分页查询");
        PageResult page = dishService.pageQuery(dishPageQueryDTO);
        return Result.success(page);
    }

    @DeleteMapping
    @ApiOperation(value = "菜品批量删除")
    public Result deleteDish(@RequestParam List<Long> ids){
        dishService.deleteDish(ids);
        return Result.success();
    }

    @GetMapping("/{id}")
    @ApiOperation(value = "根据id查询菜品")
    public Result<DishVO> getByDishId(@PathVariable Long id){
        return  dishService.getByDishId(id);
    }

    @PutMapping
    @ApiOperation(value = "修改菜品")
    public Result updateDish(@RequestBody DishDTO dishDTO){
        return dishService.updateDish(dishDTO);
    }

    @PostMapping("/status/{status}")
    @ApiOperation("启售或停售菜品")
    public Result startOrStopDishStatus(@PathVariable Integer status,Long id){
        return dishService.startOrStopDishStatus(status,id);
    }

    @GetMapping("/list")
    @ApiOperation("根据分类id查询菜品")
    public Result selectByCategoryId(Long categoryId){
        return dishService.selectByCategoryId(categoryId);
    }
}
