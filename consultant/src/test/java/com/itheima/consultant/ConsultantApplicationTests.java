package com.itheima.consultant;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.itheima.consultant.mapper.ShopMapper;
import com.itheima.consultant.pojo.Shop;
import com.itheima.consultant.service.ShopService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
class ConsultantApplicationTests {
    @Autowired
    private ShopMapper shopMapper;

    @Test
    void contextLoads() {
        Shop shop = shopMapper.selectOne(new LambdaQueryWrapper<Shop>().eq(Shop::getId, 1));
        System.out.println(shop);
    }

}
