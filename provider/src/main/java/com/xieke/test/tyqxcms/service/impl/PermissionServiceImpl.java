package com.xieke.test.tyqxcms.service.impl;

import com.baomidou.mybatisplus.mapper.EntityWrapper;
import com.xieke.test.tyqxcms.code.ResourceType;
import com.xieke.test.tyqxcms.dto.PermissionInfo;
import com.xieke.test.tyqxcms.entity.Permission;
import com.xieke.test.tyqxcms.ex.BusinessException;
import com.xieke.test.tyqxcms.mapper.PermissionMapper;
import com.xieke.test.tyqxcms.service.IPermissionService;
import com.baomidou.mybatisplus.service.impl.ServiceImpl;
import com.alibaba.dubbo.config.annotation.Service;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;

import java.util.List;

/**
 * <p>
 * 系统权限表 服务实现类
 * </p>
 *
 * @author Auto Generator
 * @since 2018-07-16
 */
@Service(version = "1.0.0", timeout = 60000)
public class PermissionServiceImpl extends ServiceImpl<PermissionMapper, Permission> implements IPermissionService {

    @Cacheable("permissionCache")
    @Override
    public List<Permission> getAllPermissions() {
        List<Permission> permissions = this.baseMapper.selectList(new EntityWrapper<>());
        return permissions;
    }

    @CacheEvict(value = "permissionCache", allEntries = true)
    @Override
    public Boolean savePermission(Permission permission) {
        Boolean res = false;
        if (permission.getId()==null) {
            if(permission.getParentId()==null){
                permission.setParentId(0);
                permission.setParentIds("0");
            } else {
                Permission ps = this.baseMapper.selectById(permission.getParentId());
                permission.setParentIds(ps.getParentIds()+"/"+ps.getId());
            }
            res = this.insert(permission);
        } else {
            res = this.updateById(permission);
        }
        return res;
    }

    @CacheEvict(value = "permissionCache", allEntries = true)
    @Override
    public Boolean delBatchPermission(List<Integer> ids) {
        Boolean res = false;
        //目录和菜单只能单个删除
        if(ids.size() == 1){
            Permission permission = this.selectById(ids.get(0));
            Permission con = new Permission();
            con.setParentId(permission.getId());
            List<Permission> list = this.baseMapper.selectList(new EntityWrapper<>(con));
            if(list!=null&&list.size()>0){
                throw new BusinessException("1", "有子权限不能删除！");
            }
            res = this.deleteById(ids.get(0));
        } else {
            res = this.baseMapper.deleteBatchIds(ids) > 0;
        }
        return res;
    }

    @Cacheable(value = "permissionCache")
    @Override
    public List<PermissionInfo> allPermissionInfo() {
        return this.baseMapper.allPermissionInfo();
    }

    @Cacheable(value = "permissionCache", key="'code:'+#p0")
    @Override
    public List<Permission> getMenuPermissions(String code) {
        EntityWrapper<Permission> wrapper = new EntityWrapper<>();
        Permission permission = new Permission();
        permission.setPermissionCode(code);
        wrapper.setEntity(permission);
        permission = this.selectOne(wrapper);
        Permission con = new Permission();
        con.setResourceType(ResourceType.MENU.getCode());
        con.setParentIds(permission.getParentIds()+"/"+permission.getId());
        con.setAvailable(1);
        wrapper.setEntity(con);
        return this.selectList(wrapper);
    }

    @Cacheable(value = "permissionCache")
    @Override
    public List<Permission> getAllDirectoryPermissions() {
        Permission permission = new Permission();
        permission.setResourceType(ResourceType.DIRECTORY.getCode());
        permission.setAvailable(1);
        return this.selectList(new EntityWrapper<>(permission));
    }

}