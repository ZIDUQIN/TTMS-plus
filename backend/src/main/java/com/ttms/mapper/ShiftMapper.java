package com.ttms.mapper;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.ttms.entity.Shift;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
@Mapper
public interface ShiftMapper extends BaseMapper<Shift> {
    @Select("SELECT * FROM shift WHERE employee_id = #{employeeId} AND status = 0 AND deleted = 0 LIMIT 1")
    Shift findActiveShift(@Param("employeeId") Long employeeId);
}
