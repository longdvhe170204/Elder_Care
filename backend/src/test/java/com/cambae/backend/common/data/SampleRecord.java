package com.cambae.backend.common.data;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/** Bản ghi nhóm 3 chỉ dùng cho test của AppendOnlyRepository (bảng tạo bởi migration test). */
@Entity
@Table(name = "sample_record")
@DataGroup(DataGroup.Group.RECORD)
public class SampleRecord {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String note;

    protected SampleRecord() {}

    public SampleRecord(String note) {
        this.note = note;
    }

    SampleRecord(Long id, String note) {
        this.id = id;
        this.note = note;
    }

    public Long getId() {
        return id;
    }

    public String getNote() {
        return note;
    }
}
