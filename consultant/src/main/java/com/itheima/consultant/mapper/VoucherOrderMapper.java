package com.itheima.consultant.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.itheima.consultant.pojo.Voucher;
import com.itheima.consultant.pojo.VoucherOrder;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * <p>
 *  Mapper 接口
 * </p>
 *
 * @author 虎哥
 * @since 2021-12-22
 */
@Mapper
public interface VoucherOrderMapper extends BaseMapper<VoucherOrder> {

    @Select("select voucher_id from tb_voucher_order where phone = #{phone}")
    List<Long> findByPhone(String phone);
}
