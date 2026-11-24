package com.example.attendance.exception;

/** 业务规则异常：由全局异常处理器转成页面提示，不是系统故障 */
public class BizException extends RuntimeException {
    public BizException(String message) {
        super(message);
    }
}
