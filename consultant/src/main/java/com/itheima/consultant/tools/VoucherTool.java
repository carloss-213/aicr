package com.itheima.consultant.tools;

import com.itheima.consultant.pojo.Voucher;
import com.itheima.consultant.service.VoucherService;
import dev.langchain4j.agent.tool.P;
import dev.langchain4j.agent.tool.Tool;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class VoucherTool {

    @Autowired
    private VoucherService voucherService;

    //1.工具方法: 查询商家的优惠券信息
    @Tool("根据商家名称查询商家的优惠券信息")
    public List<Voucher> findVoucherByShopName(@P("商家名称") String shopName) {
        return voucherService.findVoucherByShopName(shopName);
    }

    //2.工具方法: 查询用户拥有的优惠券信息
    @Tool("根据用户手机号查询用户拥有的优惠券信息")
    public List<Voucher> findVoucherByUserPhone(@P("用户手机号") String userPhone) {
        return voucherService.findVoucherByUserPhone(userPhone);
    }


}
