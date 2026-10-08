package com.lankaautoparts.autosparepro.supplier.model;

import com.lankaautoparts.autosparepro.inventory.model.PartCategory;
import jakarta.persistence.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

import java.util.HashSet;
import java.util.Set;

@Entity
@Data
public class Supplier {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank(message = "Supplier name is required")
    private String name;

    // Optional, but if something is entered it must actually be a phone
    // number — digits, and the usual +/-/space/() formatting characters —
    // not free text. Blank is still allowed since not every supplier record
    // needs one filled in.
    @Pattern(regexp = "^$|^[0-9+\\-\\s()]{7,15}$", message = "Contact number must be digits only (7-15 characters, + - and spaces allowed)")
    private String contactNumber;

    /** Free-text notes on parts supplied, kept for backwards compatibility. */
    private String partsSupplied;

    /**
     * The structured link Inventory's Low Stock Alerts panel actually uses
     * to find a matching supplier for a part — previously "partsSupplied"
     * was free text with no real relationship to Part/PartCategory at all.
     */
    @ElementCollection(fetch = FetchType.EAGER)
    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.VARCHAR)
    private Set<PartCategory> suppliedCategories = new HashSet<>();
}
