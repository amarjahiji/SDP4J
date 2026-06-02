package com.sdp4j.demo.runners;

import com.sdp4j.demo.ExampleDatabase;
import com.sdp4j.demo.models.Product;
import com.sdp4j.sq4j.SQ4J;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

/**
 * Column-mapping features via the {@link Product} entity: {@code @Length}-sized
 * {@code VARCHAR}s and {@code BigDecimal} columns backed by {@code NUMERIC}.
 * Highlights that decimal precision survives the insert → select round-trip.
 */
public class MappingDemo {

    private final SQ4J sq4j;

    public MappingDemo() {
        this.sq4j = ExampleDatabase.client().getSq4j();
    }

    public static void main(String[] args) {
        MappingDemo demo = new MappingDemo();

        demo.case1_insertWithExactDecimal();
        demo.case2_readBackPreservesScale();
        demo.case3_filterByPrice();
    }

    private void case1_insertWithExactDecimal() {
        Product product = new Product();
        product.setId(UUID.randomUUID());
        product.setName("Mechanical Keyboard");
        product.setDescription("87-key, hot-swappable");
        product.setPrice(new BigDecimal("129.95"));

        int inserted = sq4j.insertInto(Product.class).value(product).execute();
        System.out.println("case1 inserted=" + inserted);
    }

    private void case2_readBackPreservesScale() {
        // NUMERIC round-trips through BigDecimal without binary-float rounding:
        // 19.99 comes back as exactly 19.99.
        Product product = new Product();
        product.setId(UUID.randomUUID());
        product.setName("Desk Mat");
        product.setPrice(new BigDecimal("19.99"));
        sq4j.insertInto(Product.class).value(product).execute();

        List<Product> rows = sq4j.select("id", "name", "price")
                .from(Product.class)
                .where("id = :id")
                .set(":id", product.getId())
                .mapTo(Product.class);
        rows.forEach(p -> System.out.println("case2 price=" + p.getPrice()));
    }

    private void case3_filterByPrice() {
        // BigDecimal binds straight through as a named parameter.
        List<Product> affordable = sq4j.select("id", "name", "price")
                .from(Product.class)
                .where("price <= :max AND discontinued = :discontinued")
                .set(":max", new BigDecimal("50.00"))
                .set(":discontinued", false)
                .orderBy("price ASC")
                .mapTo(Product.class);
        System.out.println("case3 rows=" + affordable.size());
    }
}
