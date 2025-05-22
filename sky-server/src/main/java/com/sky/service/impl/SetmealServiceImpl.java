package com.sky.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.github.pagehelper.Page;
import com.github.pagehelper.PageHelper;
import com.sky.constant.MessageConstant;
import com.sky.constant.SpringCacheConstant;
import com.sky.constant.StatusConstant;
import com.sky.dto.SetmealDTO;
import com.sky.dto.SetmealPageQueryDTO;
import com.sky.entity.Dish;
import com.sky.entity.Setmeal;
import com.sky.entity.SetmealDish;
import com.sky.exception.SetmealEnableFailedException;
import com.sky.mapper.DishMapper;
import com.sky.mapper.SetmealDishMapper;
import com.sky.mapper.SetmealMapper;
import com.sky.result.PageResult;
import com.sky.result.Result;
import com.sky.service.SetmealService;
import com.sky.vo.DishItemVO;
import com.sky.vo.SetmealVO;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class SetmealServiceImpl extends ServiceImpl<SetmealMapper, Setmeal> implements SetmealService {
    private final SetmealMapper setmealMapper;
    private final SetmealDishMapper setmealDishMapper;
    private final DishMapper dishMapper;

    @Override
    @CacheEvict(cacheNames = SpringCacheConstant.SETMEAL_CATEGORY_CACHE
    ,key ="#setmealDTO.categoryId")
    public Result saveSetmeal(SetmealDTO setmealDTO) {
        Setmeal setmeal = new Setmeal();
        BeanUtils.copyProperties(setmealDTO, setmeal);
        //向套餐表插入数据
        save(setmeal);
        List<SetmealDish> setmealDishes = setmealDTO.getSetmealDishes();
        //获取生成的套餐id
        Long setmealId = setmeal.getId();
        setmealDishes.forEach(setmealDish -> setmealDish.setSetmealId(setmealId));
        //保存套餐和菜品的关联关系
        setmealDishMapper.insertBatch(setmealDishes);
        return Result.success();
    }

    @Override
    public PageResult pageQuery(SetmealPageQueryDTO setmealPageQueryDTO) {
        PageHelper.startPage(setmealPageQueryDTO.getPage(),setmealPageQueryDTO.getPageSize());
        Page<SetmealVO> page = setmealMapper.pageQuery(setmealPageQueryDTO);
        return new PageResult(page.getTotal(),page.getResult());
    }

    @Override
    public Result<SetmealVO> getByIdSetmeal(Long setmealId) {
        Setmeal setmeal = setmealMapper.selectById(setmealId);
        List<SetmealDish> setmealDishes = setmealDishMapper.selectList(new QueryWrapper<SetmealDish>().lambda().eq(SetmealDish::getSetmealId, setmealId));
        SetmealVO setmealVO = new SetmealVO();
        BeanUtils.copyProperties(setmeal, setmealVO);
        setmealVO.setSetmealDishes(setmealDishes);
        return Result.success(setmealVO);
    }

    @Override
    @CacheEvict(cacheNames = SpringCacheConstant.SETMEAL_CATEGORY_CACHE
            ,allEntries = true)//删除所有缓存数据
    public Result updateSetmeal(SetmealDTO setmealDTO) {
        //套餐信息跟新
        Setmeal setmeal = new Setmeal();
        BeanUtils.copyProperties(setmealDTO, setmeal);
        setmealMapper.updateById(setmeal);
        //套餐内容更新
        Long setmealId = setmealDTO.getId();
        setmealDishMapper.delete(new QueryWrapper<SetmealDish>().lambda().eq(SetmealDish::getSetmealId, setmealId));

        List<SetmealDish> setmealDishes = setmealDTO.getSetmealDishes();
        setmealDishes.forEach(setmealDish -> setmealDish.setSetmealId(setmealId));
        setmealDishMapper.insertBatch(setmealDishes);
        return Result.success();
    }

    @Override
    @CacheEvict(cacheNames = SpringCacheConstant.SETMEAL_CATEGORY_CACHE
            ,allEntries = true)//删除所有缓存数据
    public Result startOrStopStatus(Integer status, Long setmealId) {
        //起售套餐时，判断套餐内是否有停售菜品，有停售菜品提示"套餐内包含未启售菜品，无法启售"
        if (status== StatusConstant.ENABLE){
           List<Dish> list =  dishMapper.selectBySetmealId(setmealId);
            for (Dish dish : list) {
                if (dish.getStatus()==StatusConstant.DISABLE){
                    throw new SetmealEnableFailedException(MessageConstant.SETMEAL_ENABLE_FAILED);
                }
            }
        }
        Setmeal setmeal = Setmeal.builder()
                .id(setmealId)
                .status(status).build();
        setmealMapper.updateById(setmeal);

        return Result.success();
    }

    @Override
    @CacheEvict(cacheNames = SpringCacheConstant.SETMEAL_CATEGORY_CACHE
            ,allEntries = true)//删除所有缓存数据
    public Result deleteSetmeal(List<Long> ids) {
        int deleteCount = setmealMapper.deleteBatchIds(ids);
        if (deleteCount>0){
            return Result.success();
        }
        return Result.error("删除失败，请稍后重试");
    }



    @Override
    public List<DishItemVO> getDishItemById(Long id) {
        return setmealMapper.getDishItemBySetmealId(id);
    }

    //添加缓存
    @Override
    @Cacheable(cacheNames = SpringCacheConstant.SETMEAL_CATEGORY_CACHE,
    key = "#setmeal.categoryId")
    public List<Setmeal> getByCategoryId(Setmeal setmeal) {
        QueryWrapper<Setmeal> setmealQueryWrapper = new QueryWrapper<>();
        setmealQueryWrapper.lambda().eq(setmeal.getCategoryId()!=null, Setmeal::getCategoryId, setmeal.getCategoryId())
                .eq(setmeal.getStatus()!=null, Setmeal::getStatus, setmeal.getStatus());

        List<Setmeal> setmealList = setmealMapper.selectList(setmealQueryWrapper);
        return setmealList;
    }


}
