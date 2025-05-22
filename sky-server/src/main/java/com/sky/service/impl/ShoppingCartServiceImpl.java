package com.sky.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.sky.context.BaseContext;
import com.sky.dto.ShoppingCartDTO;
import com.sky.entity.Dish;
import com.sky.entity.Setmeal;
import com.sky.entity.ShoppingCart;
import com.sky.mapper.DishMapper;
import com.sky.mapper.SetmealMapper;
import com.sky.mapper.ShoppingCartMapper;
import com.sky.result.Result;
import com.sky.service.IShoppingCartService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ShoppingCartServiceImpl extends ServiceImpl<ShoppingCartMapper, ShoppingCart> implements IShoppingCartService {
    private final ShoppingCartMapper shoppingCartMapper;
    private final DishMapper dishMapper;

    private final SetmealMapper setmealMapper;

    @Override
    public Result add(ShoppingCartDTO shoppingCartDTO) {

        Long dishId = shoppingCartDTO.getDishId();
        Long setmealId = shoppingCartDTO.getSetmealId();

        //插入的时候看看购物车中是否存在
        LambdaQueryWrapper<ShoppingCart> shoppingCartLambdaQueryWrapper = new LambdaQueryWrapper<>();
        shoppingCartLambdaQueryWrapper
                .eq(ShoppingCart::getUserId,BaseContext.getCurrentId())
                .eq(shoppingCartDTO.getDishFlavor()!=null, ShoppingCart::getDishFlavor, shoppingCartDTO.getDishFlavor())
        ;
        if (setmealId!=null){
            shoppingCartLambdaQueryWrapper.eq(ShoppingCart::getSetmealId,setmealId);
        } else if (dishId!=null) {
            shoppingCartLambdaQueryWrapper.eq(ShoppingCart::getDishId,dishId);
        }
        //查询数据库，看看购物车中是否有
        ShoppingCart shoppingCartOne = shoppingCartMapper.selectOne(shoppingCartLambdaQueryWrapper);
        if (shoppingCartOne!=null){
            shoppingCartOne.setNumber(shoppingCartOne.getNumber()+1);
            shoppingCartMapper.updateById(shoppingCartOne);
            return Result.success();
        }

        //购物车中不存在
        ShoppingCart shoppingCart = new ShoppingCart();
        BeanUtils.copyProperties(shoppingCartDTO, shoppingCart);
        //本次添加到购物车的是菜品
        if (dishId!=null){
            Dish dish = dishMapper.selectById(dishId);
            shoppingCart.setName(dish.getName());
            shoppingCart.setImage(dish.getImage());
            shoppingCart.setAmount(dish.getPrice());
        }else{
            //本次添加到购物车的是套餐
            Setmeal setmeal = setmealMapper.selectById(setmealId);
            shoppingCart.setName(setmeal.getName());
            shoppingCart.setImage(setmeal.getImage());
            shoppingCart.setAmount(setmeal.getPrice());
        }


        shoppingCart.setNumber(1);
        shoppingCart.setUserId(BaseContext.getCurrentId());
        int insertNum = shoppingCartMapper.insert(shoppingCart);
        if (insertNum>0){
            return Result.success();
        }
        return Result.error("插入重复");
    }

    @Override
    public Result<List<ShoppingCart>> showShoppingCartList() {
        LambdaQueryWrapper<ShoppingCart> shoppingCartLambdaQueryWrapper = new LambdaQueryWrapper<>();
        shoppingCartLambdaQueryWrapper
                .eq(ShoppingCart::getUserId,BaseContext.getCurrentId());
        ;
        List<ShoppingCart> shoppingCarts = shoppingCartMapper.selectList(shoppingCartLambdaQueryWrapper);
        return Result.success(shoppingCarts);
    }

    @Override
    public Result cleanShoppingCart() {
        LambdaQueryWrapper<ShoppingCart> shoppingCartLambdaQueryWrapper = new LambdaQueryWrapper<>();
        shoppingCartLambdaQueryWrapper
                .eq(ShoppingCart::getUserId,BaseContext.getCurrentId());
        ;
        int delete = shoppingCartMapper.delete(shoppingCartLambdaQueryWrapper);
        return Result.success();
    }

    @Override
    public Result deleteShoppingCart(ShoppingCartDTO shoppingCartDTO) {
        LambdaQueryWrapper<ShoppingCart> shoppingCartLambdaQueryWrapper = new LambdaQueryWrapper<>();
        shoppingCartLambdaQueryWrapper.eq(ShoppingCart::getUserId, BaseContext.getCurrentId());
        shoppingCartLambdaQueryWrapper.eq(shoppingCartDTO.getDishId()!=null, ShoppingCart::getDishId, shoppingCartDTO.getDishId())
                .eq(shoppingCartDTO.getSetmealId()!=null, ShoppingCart::getSetmealId, shoppingCartDTO.getSetmealId())
                .eq(shoppingCartDTO.getDishFlavor()!=null, ShoppingCart::getDishFlavor, shoppingCartDTO.getDishFlavor());
        ShoppingCart shoppingCart = shoppingCartMapper.selectOne(shoppingCartLambdaQueryWrapper);

        //需要判断减少的是菜品，还是套餐
        if (shoppingCart!=null){
            Integer number = shoppingCart.getNumber();
            if (number>1){
                shoppingCart.setNumber(number-1);
                shoppingCartMapper.updateById(shoppingCart);
            }else{
                shoppingCartMapper.deleteById(shoppingCart.getId());
            }
        }
        return Result.success();
    }
}
