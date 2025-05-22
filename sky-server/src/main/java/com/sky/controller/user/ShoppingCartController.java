package com.sky.controller.user;

import com.sky.dto.ShoppingCartDTO;
import com.sky.entity.ShoppingCart;
import com.sky.result.Result;
import com.sky.service.IShoppingCartService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController("shoppingCartController")
@RequestMapping("/user/shoppingCart")
@Api(tags = "C端-购物车接口")
@Slf4j
@RequiredArgsConstructor
public class ShoppingCartController {
    private final IShoppingCartService shoppingCartService;
    @PostMapping("/add")
    @ApiOperation("添加进购物车")
    public Result add(@RequestBody ShoppingCartDTO shoppingCartDTO){
        log.debug("添加购物车{}",shoppingCartDTO);
        return shoppingCartService.add(shoppingCartDTO);
    }

    @GetMapping("/list")
    @ApiOperation("查询购物车")
    public Result<List<ShoppingCart>> list(){
        return shoppingCartService.showShoppingCartList();
    }

    @DeleteMapping("/clean")
    @ApiOperation("清空购物车")
    public Result clean(){
        return shoppingCartService.cleanShoppingCart();
    }

    /**
     * 删除购物车中一个商品
     * @param shoppingCartDTO
     * @return
     */
    @PostMapping("/sub")
    @ApiOperation("删除购物车中一个商品")
    public Result deleteShoppingCart(@RequestBody ShoppingCartDTO shoppingCartDTO){
        return shoppingCartService.deleteShoppingCart(shoppingCartDTO);
    }
}
