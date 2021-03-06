/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.mosmith.tools.report.website.storage.template;

import javax.persistence.Basic;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.Id;
import javax.persistence.Lob;

/**
 *
 * @author Administrator
 */
@Entity(name="Template")
public class TemplateEntity {
    private String id;
    private String groupId; // 目录ID, template ID
    private String name; // 模板名字, template name
    private String path; // 模板路径, template path
    private Boolean group; // 是否是目录, whether is group or not
    private byte[] data; // 模板的数据, template data
    
    @Id
    @Column(name="F_ID")
    public String getId() {
        return id;
    }

    public void setId(String Id) {
        this.id = Id;
    }

    @Basic
    @Column(name="F_GROUP_ID")
    public String getGroupId() {
        return groupId;
    }

    public void setGroupId(String groupId) {
        this.groupId = groupId;
    }

    @Basic
    @Column(name="F_NAME")
    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    @Basic
    @Column(name="F_PATH")
    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }

    @Basic
    @Column(name="F_IS_GROUP")
    public Boolean isGroup() {
        return group;
    }

    public void setGroup(Boolean group) {
        this.group = group;
    }

    @Lob
    @Basic(fetch = FetchType.LAZY)
    @Column(name="F_DATA")
    public byte[] getData() {
        return data;
    }

    public void setData(byte[] data) {
        this.data = data;
    }
    
    
}
