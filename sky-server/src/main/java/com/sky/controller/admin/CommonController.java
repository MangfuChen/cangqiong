package com.sky.controller.admin;

import com.sky.constant.MessageConstant;
import com.sky.result.Result;
import com.sky.utils.AliOssUtil;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.UUID;

@RestController
@Slf4j
@RequestMapping("/admin/common")
@Api(value = "通用接口")
@RequiredArgsConstructor
public class CommonController {
    private final AliOssUtil aliOssUtil;
    @PostMapping("/upload")
    @ApiOperation(value = "文件上传")
    public Result<String> upLoad(MultipartFile file){
        log.info("文件上传{}",file);
        try {
            String orifinalFilename = file.getOriginalFilename();
            String extention = orifinalFilename.substring(orifinalFilename.lastIndexOf("."));
            String objName = UUID.randomUUID() + extention;
            String upload = aliOssUtil.upload(file.getBytes(), objName);
            return Result.success(upload);
        } catch (IOException e) {
            log.error("文件上传失败",e);
        }
        return Result.error(MessageConstant.UPLOAD_FAILED);
    }


}
