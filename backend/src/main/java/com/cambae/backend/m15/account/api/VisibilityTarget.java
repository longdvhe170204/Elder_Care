package com.cambae.backend.m15.account.api;

/**
 * DTO có trường {@link FieldGroup} cài interface này để giới hạn trường biết đối tượng của DTO, xét phạm vi của từng
 * vai trò (002 research A-08; FR-034; contracts/module-interfaces.md mục 1.4). DTO không cài thì đối tượng là
 * {@link Target#UNSPECIFIED}: vai trò theo phân công không xem được nhóm có giới hạn.
 */
public interface VisibilityTarget {

    Target visibilityTarget();
}
