package com.sky.controller.admin;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.sky.constant.MessageConstant;
import com.sky.dto.CategoryDTO;
import com.sky.dto.CategoryPageQueryDTO;
import com.sky.entity.Category;
import com.sky.entity.Employee;
import com.sky.exception.DeletionNotAllowedException;
import com.sky.result.PageResult;
import com.sky.result.Result;
import com.sky.service.CategoryService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/admin/category")
@Slf4j
@Api(value = "分类接口")
public class CateGoryController {
    @Autowired
    private CategoryService categoryService;

    @PostMapping
    @ApiOperation(value = "分类添加")
    public Result save(@RequestBody CategoryDTO categoryDTO){
        log.info("分类添加{}",categoryDTO);
        return categoryService.saveCate(new Category(),categoryDTO);
    }

    @GetMapping("/page")
    @ApiOperation(value = "员工分页查询")
    public Result<PageResult> page(CategoryPageQueryDTO categoryPageQueryDTO){
        PageResult pageResult = categoryService.pageQuery(categoryPageQueryDTO);
        return Result.success(pageResult);
    }

    @PostMapping("/status/{status}")
    @ApiOperation(value = "分类启用或禁用")
    public Result startOrStop(@PathVariable Integer status,Long id){
        log.info("分类启用或禁用{},{}",status,id);
        return categoryService.startOrStop(new Category(),status,id);
    }


    @GetMapping("/{id}")
    @ApiOperation(value = "根据id查询分类")
    public Result<Category> getByid(@PathVariable Long id){
        Category category = categoryService.getById(id);
        return Result.success(category);
    }

    @PutMapping("")
    @ApiOperation(value = "修改分类信息")
    public Result updateCategory(@RequestBody CategoryDTO categoryDTO){
        return categoryService.updateCategory(new Category(),categoryDTO);
    }

    @DeleteMapping("")
    @ApiOperation(value = "根据id删除分类")
    public Result deleteByid(Long id){
        log.info("删除分类{}",id);
        long count = categoryService.count(new QueryWrapper<Category>().eq("id", id));
        if (count>0){
            throw new DeletionNotAllowedException(MessageConstant.CATEGORY_BE_RELATED_BY_SETMEAL);
        }
        boolean flag = categoryService.removeById(id);
        if (flag){
            return Result.success();
        }
        return Result.error("删除失败");
    }

    @GetMapping("/list")
    @ApiOperation(value = "根据类型查询分类")
    public Result<PageResult> pageByType(CategoryPageQueryDTO categoryPageQueryDTO){
        PageResult pageResult = categoryService.pageQuery(categoryPageQueryDTO);
        return Result.success(pageResult);
    }
}
