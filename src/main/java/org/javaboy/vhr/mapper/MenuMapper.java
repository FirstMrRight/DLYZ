package org.javaboy.vhr.mapper;

import org.javaboy.vhr.model.Hr;
import org.javaboy.vhr.model.Menu;

import java.util.List;

public interface MenuMapper {
    int deleteByPrimaryKey(Integer id);

    int insert(Menu record);

    int insertSelective(Menu record);

    Menu selectByPrimaryKey(Integer id);

    int updateByPrimaryKeySelective(Menu record);

    int updateByPrimaryKey(Menu record);

    List<Menu> getMenuByHrId(Integer id);

    List<Menu> getAllMenusWithRole();

    List<Menu> getAllmenus();

    List<Integer> getMidsByRid(Integer rid);

}
