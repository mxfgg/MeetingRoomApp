package com.example.meetingroomapp;

import com.example.meetingroomapp.data.model.SimpleResult;
import org.junit.Test;
import static org.junit.Assert.*;

public class SimpleResultTest {

    @Test
    public void success_isSuccessTrue() {
        SimpleResult r = SimpleResult.success();
        assertTrue(r.isSuccess());
    }

    @Test
    public void success_errorMessageIsNull() {
        SimpleResult r = SimpleResult.success();
        assertNull(r.getErrorMessage());
    }

    @Test
    public void error_isSuccessFalse() {
        SimpleResult r = SimpleResult.error("something went wrong");
        assertFalse(r.isSuccess());
    }

    @Test
    public void error_errorMessageSet() {
        SimpleResult r = SimpleResult.error("something went wrong");
        assertEquals("something went wrong", r.getErrorMessage());
    }

    @Test
    public void error_nullMessageAllowed() {
        SimpleResult r = SimpleResult.error(null);
        assertFalse(r.isSuccess());
        assertNull(r.getErrorMessage());
    }

    @Test
    public void successAndError_areDistinct() {
        SimpleResult s = SimpleResult.success();
        SimpleResult e = SimpleResult.error("fail");
        assertNotEquals(s.isSuccess(), e.isSuccess());
    }
}
