package com.ttms.mapper;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.ttms.entity.Coupon;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;
import java.util.List;
@Mapper
public interface CouponMapper extends BaseMapper<Coupon> {
    @Select("SELECT * FROM coupon WHERE status = 1 AND remaining_qty > 0 AND deleted = 0")
    List<Coupon> selectAvailable();
    @Update("UPDATE coupon SET remaining_qty = remaining_qty - 1 WHERE id = #{id} AND remaining_qty > 0")
    int decrementQty(@Param("id") Long id);
}
