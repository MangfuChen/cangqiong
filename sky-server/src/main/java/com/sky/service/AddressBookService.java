package com.sky.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.sky.entity.AddressBook;
import com.sky.result.Result;

import java.util.List;

public interface AddressBookService extends IService<AddressBook> {
    void setDefault(AddressBook addressBook);

    Result<AddressBook> getAddressDefault();

    Result<List<AddressBook>> getAddressList();

    Result insertAddress(AddressBook addressBook);
}
