package com.sky.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.sky.dto.SetmealDTO;
import com.sky.dto.SetmealPageQueryDTO;
import com.sky.entity.Setmeal;
import com.sky.result.PageResult;
import com.sky.result.Result;
import com.sky.vo.DishItemVO;
import com.sky.vo.SetmealVO;

import java.util.List;

public interface SetmealService extends IService<Setmeal> {
    Result saveSetmeal(SetmealDTO setmealDTO);

    PageResult pageQuery(SetmealPageQueryDTO setmealPageQueryDTO);

    Result<SetmealVO> getByIdSetmeal(Long id);

    Result updateSetmeal(SetmealDTO setmealDTO);

    Result startOrStopStatus(Integer status, Long id);

    Result deleteSetmeal(List<Long> id);

    List<DishItemVO> getDishItemById(Long id);

    List<Setmeal> getByCategoryId(Setmeal setmeal);
}
