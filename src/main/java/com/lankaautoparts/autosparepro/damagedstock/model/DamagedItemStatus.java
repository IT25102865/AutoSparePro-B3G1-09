package com.lankaautoparts.autosparepro.damagedstock.model;

public enum DamagedItemStatus {
    /** A customer just filed this claim; nobody has looked at it yet. */
    REPORTED,
    /** Staff confirmed the item really is damaged; awaiting refund/replace. */
    VERIFIED,
    /** Verified, and the linked payment was refunded. Terminal. */
    REFUNDED,
    /** Verified, and a replacement unit was issued from stock. Terminal. */
    REPLACED,
    /** The claim was found invalid and denied. Terminal. */
    REJECTED
}
