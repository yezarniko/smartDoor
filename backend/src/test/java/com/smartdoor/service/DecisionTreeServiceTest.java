package com.smartdoor.service;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DecisionTreeServiceTest {
    @Test
    void authorizesLowRiskPermittedStaff() throws Exception {
        DecisionTreeService service = new DecisionTreeService();
        service.train();
        assertEquals("AUTHORIZED", service.predict(true, true, true, "STAFF", "LOW").result());
        assertTrue(service.info().trainingRows() > 10);
    }

    @Test
    void rejectsWrongDoor() throws Exception {
        DecisionTreeService service = new DecisionTreeService();
        service.train();
        assertEquals("UNAUTHORIZED", service.predict(false, true, true, "STAFF", "LOW").result());
    }
}

