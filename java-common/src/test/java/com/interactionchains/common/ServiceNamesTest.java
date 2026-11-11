package com.interactionchains.common;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class ServiceNamesTest {

    @Test
    void exposesStableServiceNames() {
        assertEquals("interaction-chains", ServiceNames.APPLICATION);
        assertEquals("python-service", ServiceNames.PYTHON_SERVICE);
    }
}
