package com.ttms.mapper;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.ttms.entity.UserCoupon;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;
import java.util.List;
@Mapper
public interface UserCouponMapper extends BaseMapper<UserCoupon> {
    @Select("SELECT uc.*, c.name, c.type, c.value, c.min_order_amount FROM user_coupon uc JOIN coupon c ON uc.coupon_id = c.id WHERE uc.user_id = #{userId} AND uc.status = 0 AND uc.expire_time > NOW()")
    List<UserCoupon> selectUserAvailable(@Param("userId") Long userId);
    @Update("UPDATE user_coupon SET status = 1, used_order_id = #{orderId}, use_time = NOW() WHERE id = #{id} AND status = 0")
    int useCoupon(@Param("id") Long id, @Param("orderId") Long orderId);
}
