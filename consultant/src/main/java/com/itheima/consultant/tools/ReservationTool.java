package com.itheima.consultant.tools;

import com.itheima.consultant.pojo.Reservation;
import com.itheima.consultant.pojo.Shop;
import com.itheima.consultant.service.ReservationService;
import com.itheima.consultant.service.ShopService;
import dev.langchain4j.agent.tool.P;
import dev.langchain4j.agent.tool.Tool;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;

@Component
public class ReservationTool {

    @Autowired
    private ReservationService reservationService;

    //1.工具方法: 添加预约信息
    @Tool("预约到店消费服务")
    public void addReservation(
            @P("用户姓名") String name,
            @P("用户手机号") String phone,
            @P("预约到店消费时间,格式为: yyyy-MM-dd'T'HH:mm") String communicationTime,
            @P("预约指定的商家") String shopName
    ) {
        Reservation reservation = new Reservation(null, name, phone, LocalDateTime.parse(communicationTime), shopName);
        reservationService.insert(reservation);
    }

    //2.工具方法: 查询预约信息
    @Tool("根据用户手机号查询预约单")
    public List<Reservation> findReservation(@P("用户手机号") String phone) {
        return reservationService.findByPhone(phone);
    }

}
