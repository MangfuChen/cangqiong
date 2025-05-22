package com.sky.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.github.pagehelper.Page;
import com.sky.dto.DishPageQueryDTO;
import com.sky.entity.Dish;
import com.sky.vo.DishVO;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;
import java.util.Map;

@Mapper
public interface DishMapper extends BaseMapper<Dish> {
    Page<DishVO> pageQuery(DishPageQueryDTO dishPageQueryDTO);

    List<Dish> selectBySetmealId(Long setmealId);

	Integer countByMap(Map map);
}
