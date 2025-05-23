package com.sky.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.github.pagehelper.Page;
import com.sky.dto.SetmealPageQueryDTO;
import com.sky.entity.Setmeal;
import com.sky.vo.DishItemVO;
import com.sky.vo.SetmealVO;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;
import java.util.Map;

@Mapper
public interface SetmealMapper extends BaseMapper<Setmeal> {
    Page<SetmealVO> pageQuery(SetmealPageQueryDTO setmealPageQueryDTO);

    List<DishItemVO> getDishItemBySetmealId(Long id);

	Integer countByMap(Map map);
}
