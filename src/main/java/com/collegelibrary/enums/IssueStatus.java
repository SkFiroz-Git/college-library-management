package com.collegelibrary.enums;

/**
 * Status of a borrowing record.
 * NOTE: overdue is NOT stored here. A record is overdue when
 * status == ISSUED and dueDate < today. Storing it would become
 * stale/wrong after midnight, so we always calculate it.
 */
public enum IssueStatus {
    ISSUED,
    RETURNED
}
