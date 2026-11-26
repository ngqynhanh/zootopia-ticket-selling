package com.zoopass.catalog;

/** Groups ticket types on the price page. */
public enum TicketCategory {
    /** Gate entry (adult / child / under 1 m). */
    GATE,
    /** Train / electric train loop. */
    TRAIN,
    /** Private or VIP electric-car tour (minimum 6 tickets). */
    TOUR,
    /** Ticket book (10 entries) or monthly pass. */
    PASS,
    /** Extra services such as a tour guide. */
    SERVICE
}
