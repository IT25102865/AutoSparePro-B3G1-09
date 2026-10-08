package com.lankaautoparts.autosparepro.payment.model;

import jakarta.persistence.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import lombok.Data;

import java.time.LocalDate;

/**
 * A payment record for an approved request or cart checkout.
 *
 * IMPORTANT (security note): this application never stores a full card
 * number or CVV — only the last 4 digits of the card are kept. Do not
 * extend this entity to store full card numbers, CVV/CVC codes, or expiry
 * dates.
 */
@Entity
@Data
public class Payment {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long requestId;
    private String invoiceNumber;
    private String cardHolderName;
    private String cardLast4;
    private double amount;
    private String username;
    private LocalDate paymentDate;

    @Enumerated(EnumType.STRING)

    @JdbcTypeCode(SqlTypes.VARCHAR)
    private PaymentStatus status;

    @Enumerated(EnumType.STRING)

    @JdbcTypeCode(SqlTypes.VARCHAR)
    private CardType cardType;

    /** "CREDIT" or "DEBIT" — kept as a plain string to match the admin edit form. */
    private String paymentMethod;
}
