package com.zoopass.audit;

/** Actions written to the audit log. */
public enum AuditAction {
    USER_REGISTERED,
    EMAIL_VERIFIED,
    LOGIN_SUCCEEDED,
    LOGIN_FAILED,
    LOGOUT,
    PASSWORD_CHANGED,
    PASSWORD_RESET,
    PROFILE_UPDATED,
    USER_CREATED,
    USER_UPDATED,
    USER_ROLE_CHANGED,
    USER_DEACTIVATED,
    USER_REACTIVATED,
    CREATED,
    UPDATED,
    DELETED,
    RESTORED
}
