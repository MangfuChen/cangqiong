package com.sky.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.github.pagehelper.Page;
import com.github.pagehelper.PageHelper;
import com.sky.annotation.AutoFill;
import com.sky.context.BaseContext;
import com.sky.dto.CategoryDTO;
import com.sky.dto.CategoryPageQueryDTO;
import com.sky.entity.Category;
import com.sky.enumeration.OperationType;
import com.sky.mapper.CategoryMapper;
import com.sky.result.PageResult;
import com.sky.result.Result;
import com.sky.service.CategoryService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class CategoryServiceImple extends ServiceImpl<CategoryMapper, Category> implements CategoryService {

    private final CategoryMapper categoryMapper;
    @Override
    @AutoFill(OperationType.INSERT)
    public Result saveCate(Category category, CategoryDTO categoryDTO) {
        BeanUtils.copyProperties(categoryDTO, category);
        category.setStatus(0);
        boolean flag = save(category);
        if (flag){
            return Result.success();
        }
        return Result.error("添加失败");
    }

    @Override
    public PageResult pageQuery(CategoryPageQueryDTO categoryPageQueryDTO) {
        PageHelper.startPage(categoryPageQueryDTO.getPage(),categoryPageQueryDTO.getPageSize());
        QueryWrapper<Category> queryWrapper = new QueryWrapper<>();
        queryWrapper.lambda().
                like(categoryPageQueryDTO.getName()!=null,Category::getName,categoryPageQueryDTO.getName())
                .eq(categoryPageQueryDTO.getType()!=null, Category::getType,categoryPageQueryDTO.getType())
        ;
        Page<Category> page = (Page<Category>)categoryMapper.selectList(queryWrapper);
        return new PageResult(page.getTotal(),page.getResult());
    }

    @Override
    @AutoFill(OperationType.UPDATE)
    public Result startOrStop(Category category, Integer status, Long id) {
        category.setId(id);
        category.setStatus(status);
        boolean flag = updateById(category);
        if (flag){
            return Result.success();
        }
        return Result.error("修改失败");
    }

    @Override
    @AutoFill(OperationType.UPDATE)
    public Result updateCategory(Category category, CategoryDTO categoryDTO) {
        BeanUtils.copyProperties(categoryDTO, category);
        boolean flag = updateById(category);
        if (flag){
            return Result.success();
        }
        return Result.error("修改失败");
    }

    @Override
    public List<Category> pageQueryList(CategoryPageQueryDTO categoryPageQueryDTO) {
        QueryWrapper<Category> queryWrapper = new QueryWrapper<>();
        queryWrapper.lambda().
                like(categoryPageQueryDTO.getName()!=null,Category::getName,categoryPageQueryDTO.getName())
                .eq(categoryPageQueryDTO.getType()!=null, Category::getType,categoryPageQueryDTO.getType())
        ;
        List<Category> categories = categoryMapper.selectList(queryWrapper);
        return  categories;
    }
}
