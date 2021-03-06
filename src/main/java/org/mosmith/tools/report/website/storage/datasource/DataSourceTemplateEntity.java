/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.mosmith.tools.report.website.storage.datasource;

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
@Entity(name="DataSourceTemplate")
public class DataSourceTemplateEntity {
    private String id;
    private String groupId;
    private String name;
    private String path;
    private Boolean group;
    private String content;
    
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
    @Basic()
    @Column(name="F_CONTENT", length=65536)
    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }
    
    
}
