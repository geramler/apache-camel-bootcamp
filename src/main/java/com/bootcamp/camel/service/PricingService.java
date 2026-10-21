package com.bootcamp.camel.service;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Named;

/** Bean CDI invocado desde una ruta por su nombre con .method(...). */
@ApplicationScoped
@Named("pricingService")
public class PricingService {
    public double applyTax(double amount) {
        return Math.round(amount * 1.16 * 100.0) / 100.0;
    }
}
