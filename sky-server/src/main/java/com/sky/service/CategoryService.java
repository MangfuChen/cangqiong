package com.sky.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.sky.dto.CategoryDTO;
import com.sky.dto.CategoryPageQueryDTO;
import com.sky.entity.Category;
import com.sky.result.PageResult;
import com.sky.result.Result;

import java.util.List;

public interface CategoryService extends IService<Category> {
    Result saveCate(Category category, CategoryDTO categoryDTO);

    PageResult pageQuery(CategoryPageQueryDTO categoryPageQueryDTO);

    Result startOrStop(Category category, Integer status, Long id);

    Result updateCategory(Category category, CategoryDTO categoryDTO);

    List<Category> pageQueryList(CategoryPageQueryDTO categoryPageQueryDTO);
}
