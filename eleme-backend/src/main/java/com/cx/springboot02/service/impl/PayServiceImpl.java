package com.cx.springboot02.service.impl;

import com.alipay.api.AlipayApiException;
import com.cx.springboot02.common.pay.AliBean;
import com.cx.springboot02.common.pay.AliPay;
import com.cx.springboot02.common.pay.AliReturnPayBean;
import com.cx.springboot02.common.pay.PayUtil;
import com.cx.springboot02.common.utils.Final;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class PayServiceImpl  {
    @Autowired
    private AliPay aliPay;

    public String aliPay(AliBean alipayBean) throws AlipayApiException {
        System.out.println(alipayBean.toString());
        return aliPay.pay(alipayBean);
    }

    public String aliRefund(AliReturnPayBean alipayBean) throws AlipayApiException {
        System.out.println(alipayBean.toString());
        return PayUtil.aliRefund(alipayBean.getOut_trade_no(),alipayBean.getTrade_no(),alipayBean.getTotal_amount(), Final.RETURN_REASON01, alipayBean.getOut_trade_no());
    }
}