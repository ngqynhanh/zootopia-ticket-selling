package com.zoopass.user;

/**
 * User roles, stored as text in {@code users.role}. Guests are simply unauthenticated requests.
 */
public enum Role {
    /** Individual visitor or group representative. */
    CUSTOMER,
    /** Counter / gate staff: reviews documents, checks tickets in, records cash payments. */
    STAFF,
    /** Business manager: prices, combos, policies, content, reports. */
    MANAGER,
    /** System administrator: users, roles, audit log, settings. */
    ADMIN
}
