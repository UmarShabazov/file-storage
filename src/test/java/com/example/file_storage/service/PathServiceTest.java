package com.example.file_storage.service;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;


public class PathServiceTest {

    private PathService service;

    @BeforeEach
    void setUp() {
        service = new PathService();
    }
    @Test
    public void givenPath_thenReturnFileName() {

        String path = "/work/life/balance.txt";
        String pathD = "/work/life/";


        Assertions.assertEquals("balance.txt", service.extractName(path));
        Assertions.assertEquals("life/", service.extractName(pathD));

    }

    @Test
    public void givenPath_thenReturnParentPath() {

        String path = "/work/life/balance.txt";
        String pathD = "/work/life/";


        Assertions.assertEquals("/work/life/", service.extractParentPath(path));
        Assertions.assertEquals("/work/", service.extractParentPath(pathD));

    }
}
