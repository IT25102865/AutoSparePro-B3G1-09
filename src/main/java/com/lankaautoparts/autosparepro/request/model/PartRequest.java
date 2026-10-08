package com.lankaautoparts.autosparepro.request.model;

import jakarta.persistence.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import lombok.Data;

import java.time.LocalDate;

@Entity
@Data
public class PartRequest {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String partNumber;
    private String partName;
    private String username;
    private int quantity;

    @Enumerated(EnumType.STRING)

    @JdbcTypeCode(SqlTypes.VARCHAR)
    private RequestStatus status;

    private LocalDate requestedDate;
}
