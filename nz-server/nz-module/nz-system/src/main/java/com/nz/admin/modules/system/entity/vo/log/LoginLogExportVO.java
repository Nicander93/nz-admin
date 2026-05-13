package com.nz.admin.modules.system.entity.vo.log;

import com.alibaba.excel.annotation.ExcelProperty;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 登录日志导出行。
 */
@Data
public class LoginLogExportVO {

    @ExcelProperty("日志ID")
    private Long id;

    @ExcelProperty("用户名")
    private String username;

    @ExcelProperty("IP")
    private String ip;

    @ExcelProperty("状态")
    private String status;

    @ExcelProperty("消息")
    private String msg;

    @ExcelProperty("登录时间")
    private LocalDateTime loginTime;
}
