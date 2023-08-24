package com.hn2.core.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.GenericGenerator;
import org.hibernate.annotations.UpdateTimestamp;
import org.springframework.data.annotation.CreatedBy;
import org.springframework.data.annotation.LastModifiedBy;

import javax.persistence.Column;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.MappedSuperclass;
import java.io.Serializable;

/**
 * 基礎 Entity，含共用欄位 Id、Create_User、Create_Date、Modify_User、Modify_Date
 *
 */
@Data
@SuperBuilder
@MappedSuperclass
@AllArgsConstructor
@NoArgsConstructor
public class BaseEntity implements Serializable {
  private static final long serialVersionUID = 1L;
  /** uuid */
  @Id
  @GenericGenerator(name = "jpa-uuid", strategy = "uuid2")
  @GeneratedValue(generator = "jpa-uuid")
  @Column(name = "Id", columnDefinition = "uniqueidentifier")
  private String id;
  /** 建立者 */
  @CreatedBy
  @Column(name = "Create_User", updatable = false)
  private String createUser;
  /** 建立日期 */
  @CreationTimestamp
  @Column(name = "Create_Date", updatable = false)
  private java.time.LocalDateTime createDate;
  /** 修改者 */
  @LastModifiedBy
  @Column(name = "Modify_User")
  private String modifyUser;
  /** 修改日期 */
  @UpdateTimestamp
  @Column(name = "Modify_Date")
  private java.time.LocalDateTime modifyDate;
}
