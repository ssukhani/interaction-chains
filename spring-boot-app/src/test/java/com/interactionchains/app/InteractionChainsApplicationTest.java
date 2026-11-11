package com.interactionchains.app;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.interactionchains.app.analysis.EstateCatalog;
import com.interactionchains.common.ServiceNames;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
class InteractionChainsApplicationTest {

    @Autowired
    private PythonServiceProperties pythonServiceProperties;

    @Autowired
    private EstateCatalog estateCatalog;

    @Test
    void loadsPythonServiceUrlFromYaml() {
        assertEquals(ServiceNames.APPLICATION, "interaction-chains");
        assertEquals("http://localhost:8081", pythonServiceProperties.getBaseUrl());
    }

    @Test
    void loadsTheAccountOpeningEstate() {
        assertEquals(3, estateCatalog.journeys().size());
    }
}
