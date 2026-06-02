package com.sdp4j.demo.models;

import com.sdp4j.sm4j.annotations.DefaultFalse;
import com.sdp4j.sm4j.annotations.DefaultNumeric;
import com.sdp4j.sm4j.annotations.Length;
import com.sdp4j.sm4j.annotations.NotNull;
import com.sdp4j.sm4j.annotations.PrimaryKey;
import com.sdp4j.sm4j.annotations.Table;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Showcases the column-mapping annotations:
 * <ul>
 *   <li>{@link Length} — {@code name} becomes {@code VARCHAR(120)},
 *       {@code description} {@code VARCHAR(2000)} (instead of the default 255).</li>
 *   <li>{@code BigDecimal} — {@code price} maps to {@code NUMERIC}, preserving
 *       exact decimal precision.</li>
 *   <li>{@link DefaultNumeric} — {@code price} defaults to {@code 0.00}.</li>
 * </ul>
 */
@Table(name = "products", mapToSnakeCase = true)
public class Product {

    @PrimaryKey
    private UUID id;

    @NotNull
    @Length(120)
    private String name;

    @Length(2000)
    private String description;

    @NotNull
    @DefaultNumeric("0.00")
    private BigDecimal price;

    @DefaultFalse
    private Boolean discontinued;

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public BigDecimal getPrice() {
        return price;
    }

    public void setPrice(BigDecimal price) {
        this.price = price;
    }

    public Boolean getDiscontinued() {
        return discontinued;
    }

    public void setDiscontinued(Boolean discontinued) {
        this.discontinued = discontinued;
    }
}
