package com.sky.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.github.pagehelper.Page;
import com.github.pagehelper.PageHelper;
import com.sky.constant.MessageConstant;
import com.sky.constant.RedisConstant;
import com.sky.constant.StatusConstant;
import com.sky.dto.DishDTO;
import com.sky.dto.DishPageQueryDTO;
import com.sky.entity.Dish;
import com.sky.entity.DishFlavor;
import com.sky.entity.Setmeal;
import com.sky.exception.DeletionNotAllowedException;
import com.sky.mapper.DishFlavorMapper;
import com.sky.mapper.DishMapper;
import com.sky.mapper.SetmealDishMapper;
import com.sky.mapper.SetmealMapper;
import com.sky.result.PageResult;
import com.sky.result.Result;
import com.sky.service.DishService;
import com.sky.service.SetmealService;
import com.sky.vo.DishVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

@Service
@Slf4j
@RequiredArgsConstructor
public class DishServiceImpl extends ServiceImpl<DishMapper, Dish> implements DishService {

    private final DishMapper dishMapper;
    private final RedisTemplate redisTemplate;

    private final DishFlavorMapper dishFlavorMapper;
    private final SetmealDishMapper setmealDishMapper;
    private final SetmealMapper setmealMapper;

    @Override
    @Transactional
    public Result saveWithFlavor(DishDTO dishDTO) {
        //rediskey——这里面的数据为这个分类id下所有的菜品
        String redisDishKey = RedisConstant.DISH_CATEGORY+dishDTO.getCategoryId();
        //清除受影响的缓存
        redisTemplate.delete(redisDishKey);

        Dish dish = new Dish();
        BeanUtils.copyProperties(dishDTO,dish);

        int insert = dishMapper.insert(dish);
        List<DishFlavor> flavors = dishDTO.getFlavors();
        Long id = dish.getId();
        log.info("菜品id{}",id);
        if (flavors!=null&&flavors.size()>0){
            //给每一个口味设置一个dishID
            flavors.forEach(dishFlavor -> dishFlavor.setDishId(id));
            dishFlavorMapper.insertBatch(flavors);
        }
        if (insert>0){
            return Result.success();
        }
        return Result.error("添加失败");
    }

    @Override
    public PageResult pageQuery(DishPageQueryDTO dishPageQueryDTO) {
        PageHelper.startPage(dishPageQueryDTO.getPage(), dishPageQueryDTO.getPageSize());
        Page<DishVO> pageVo = dishMapper.pageQuery(dishPageQueryDTO);
        return new PageResult(pageVo.getTotal(),pageVo.getResult());
    }

    @Override
    public void deleteDish(List<Long> dishIds) {

        //起售中不能删除
        for (Long id : dishIds) {
            Dish dish = dishMapper.selectById(id);
            if (dish.getStatus()== StatusConstant.ENABLE){
                throw new DeletionNotAllowedException(MessageConstant.DISH_ON_SALE);
            }
        }
        //菜品与套餐有关联也不能删除
        List<Long> setmealIdsByDishIds = setmealDishMapper.getSetmealIdsByDishIds(dishIds);
        if (setmealIdsByDishIds!=null&&setmealIdsByDishIds.size()>0){
            throw new DeletionNotAllowedException(MessageConstant.DISH_BE_RELATED_BY_SETMEAL);
        }
        //删除菜品
        dishMapper.deleteBatchIds(dishIds);
        //删除菜品的口味
        for (Long id : dishIds) {
            dishFlavorMapper.deleteByDishId(id);
        }
        //删除缓存
        String redisDishKey = RedisConstant.DISH_ALL;
        clearCashe(redisDishKey);

    }

    @Override
    public Result<DishVO> getByDishId(Long id) {
        DishVO dishVO = new DishVO();
        Dish dish = dishMapper.selectById(id);
        List<DishFlavor> dishFlavors = dishFlavorMapper.selectList(new QueryWrapper<DishFlavor>().lambda().eq(DishFlavor::getDishId, id));
        BeanUtils.copyProperties(dish, dishVO);
        dishVO.setFlavors(dishFlavors);
        return Result.success(dishVO);
    }

    @Override
    public Result updateDish(DishDTO dishDTO) {
        Dish dish = new Dish();
        BeanUtils.copyProperties(dishDTO, dish);
        int updateCount = dishMapper.updateById(dish);
        dishFlavorMapper.deleteByDishId(dishDTO.getId());

        //更新口味
        List<DishFlavor> flavors = dishDTO.getFlavors();
        Long dishId = dishDTO.getId();
        if (flavors!=null&&flavors.size()>0){
            flavors.forEach(dishFlavor -> dishFlavor.setDishId(dishId));
            dishFlavorMapper.insertBatch(flavors);
        }
        //通一删除所有dish数据
        //删除缓存
        String redisDishKey = RedisConstant.DISH_ALL;
        clearCashe(redisDishKey);
        return Result.success();
    }

    @Override
    public Result startOrStopDishStatus(Integer status, Long id) {
        Dish dish = Dish.builder()
                .id(id)
                .status(status)
                .build();
        dishMapper.updateById(dish);
        //菜品挺售，套餐也必须停售
        if (status==StatusConstant.DISABLE){
            ArrayList<Long> dishIds = new ArrayList<>();
            dishIds.add(id);
            List<Long> setmealIdsByDishIds = setmealDishMapper.getSetmealIdsByDishIds(dishIds);
            if (setmealIdsByDishIds!=null&&setmealIdsByDishIds.size()>0){
                for (Long setmealIdsByDishId : setmealIdsByDishIds) {
                    Setmeal setmeal = Setmeal.builder()
                            .id(setmealIdsByDishId)
                            .status(StatusConstant.DISABLE)
                            .build();
                    setmealMapper.updateById(setmeal);
                }
            }
        }
        //通一删除所有dish数据
        //删除缓存
        String redisDishKey = RedisConstant.DISH_ALL;
        clearCashe(redisDishKey);
        return Result.success();
    }

    @Override
    public Result selectByCategoryId(Long categoryId) {
        QueryWrapper<Dish> dishQueryWrapper = new QueryWrapper<>();
        dishQueryWrapper.lambda()
                .eq(categoryId!=null, Dish::getCategoryId, categoryId)
                .eq(Dish::getStatus, StatusConstant.ENABLE);
        List<Dish> dishes = dishMapper.selectList(dishQueryWrapper);
        return Result.success(dishes);
    }

    /**
     * 根据分类id查询菜品
     * @param dish
     * @return
     */
    @Override
    public List<DishVO> listWithFlavor(Dish dish) {
        //先查询redis
        String redisDishKey = RedisConstant.DISH_CATEGORY+dish.getCategoryId();

        List<DishVO> redisListDishVO = (List<DishVO>) redisTemplate.opsForValue().get(redisDishKey);

        if (redisListDishVO!=null&&redisListDishVO.size()>0){
            return redisListDishVO;
        }

        //数据库获取
        List<DishVO> dishVOList = new ArrayList<>();
        QueryWrapper<Dish> dishQueryWrapper = new QueryWrapper<>();
        dishQueryWrapper.lambda()
                .eq(dish.getCategoryId()!=null, Dish::getCategoryId, dish.getCategoryId())
                .eq(Dish::getStatus, StatusConstant.ENABLE);
        List<Dish> dishList = dishMapper.selectList(dishQueryWrapper);

        for (Dish d : dishList) {
            DishVO dishVO = new DishVO();
            BeanUtils.copyProperties(d,dishVO);

            //根据菜品id查询对应的口味
            List<DishFlavor> flavors = dishFlavorMapper.selectList(new QueryWrapper<DishFlavor>().lambda()
                    .eq(DishFlavor::getDishId, dishVO.getId()));

            dishVO.setFlavors(flavors);
            dishVOList.add(dishVO);
        }

        //redis中没有获取，存入redis中
        redisTemplate.opsForValue().set(redisDishKey, dishVOList);
        return dishVOList;
    }


    private void clearCashe(String pattern){
        Set keys = redisTemplate.keys(pattern);
        redisTemplate.delete(keys);
    }
}


