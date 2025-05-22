package com.sky.service.impl;

import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.sky.context.BaseContext;
import com.sky.entity.AddressBook;
import com.sky.mapper.AddressBookMapper;
import com.sky.result.Result;
import com.sky.service.AddressBookService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class AddressBookServiceImpl extends ServiceImpl<AddressBookMapper, AddressBook> implements AddressBookService {
    private final AddressBookMapper addressBookMapper;
    @Override
    public void setDefault(AddressBook addressBook) {

        //1、将当前用户的所有地址修改为非默认地址
        addressBook.setIsDefault(0);
        addressBook.setUserId(BaseContext.getCurrentId());
        //=用户id
        LambdaUpdateWrapper<AddressBook> lambdaUpdateWrapper = new LambdaUpdateWrapper<AddressBook>()
                .eq(AddressBook::getUserId, BaseContext.getCurrentId());
        addressBookMapper.update(addressBook,
                lambdaUpdateWrapper
        );
        //2、将当前地址改为默认地址
        addressBook.setIsDefault(1);
        addressBookMapper.update(addressBook,
                lambdaUpdateWrapper
        );
    }

    @Override
    public Result<AddressBook> getAddressDefault() {
        //=用户id
        LambdaUpdateWrapper<AddressBook> lambdaUpdateWrapper = new LambdaUpdateWrapper<AddressBook>()
                .eq(AddressBook::getUserId, BaseContext.getCurrentId())
                .eq(AddressBook::getIsDefault, 1);
        AddressBook addressBook = addressBookMapper.selectOne(lambdaUpdateWrapper);
        if (addressBook!=null){
            return Result.success(addressBook);
        }
        return Result.error("没有默认地址");
    }

    @Override
    public Result<List<AddressBook>> getAddressList() {
        LambdaUpdateWrapper<AddressBook> lambdaUpdateWrapper = new LambdaUpdateWrapper<AddressBook>()
                .eq(AddressBook::getUserId, BaseContext.getCurrentId());
        List<AddressBook> addressBooks = addressBookMapper.selectList(lambdaUpdateWrapper);

        return Result.success(addressBooks);
    }

    @Override
    public Result insertAddress(AddressBook addressBook) {
        addressBook.setUserId(BaseContext.getCurrentId());
        int insert = addressBookMapper.insert(addressBook);
        if (insert>0){
            return Result.success();
        }
        return Result.error("插入失败");
    }
}
