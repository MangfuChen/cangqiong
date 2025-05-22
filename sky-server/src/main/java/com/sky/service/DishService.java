package com.sky.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.sky.dto.DishDTO;
import com.sky.dto.DishPageQueryDTO;
import com.sky.entity.Dish;
import com.sky.result.PageResult;
import com.sky.result.Result;
import com.sky.vo.DishVO;

import java.util.List;

public interface DishService extends IService<Dish> {
    Result saveWithFlavor(DishDTO dishDTO);

    PageResult pageQuery(DishPageQueryDTO dishPageQueryDTO);

    void deleteDish(List<Long> ids);

    Result<DishVO> getByDishId(Long id);

    Result updateDish(DishDTO dishDTO);

    Result startOrStopDishStatus(Integer status, Long id);

    Result selectByCategoryId(Long categoryId);

    List<DishVO> listWithFlavor(Dish dish);
}
