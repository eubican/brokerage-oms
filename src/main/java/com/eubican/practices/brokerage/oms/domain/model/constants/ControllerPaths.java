package com.eubican.practices.brokerage.oms.domain.model.constants;

public final class ControllerPaths {

    public static final String API_V_1_ASSETS = "/api/v1/assets";

    public static final String API_V_1_AUTH = "/api/v1/auth";

    public static final String API_V_1_ORDERS = "/api/v1/orders";

    public static final String API_V_1_ADMIN_ORDERS = "/api/v1/admin/orders";

    private ControllerPaths() {
        throw new AssertionError("Cannot instantiate utility class.");
    }

}
