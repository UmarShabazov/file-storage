package com.example.file_storage.service;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.example.file_storage.entity.ResourceType;

public class PathServiceTest {

    private PathService service;

    @BeforeEach
    void setUp() {
        service = new PathService();
    }

    @Test
    public void extractName_returnsRelativeResourceName() {
        Assertions.assertEquals("", service.extractName(""));
        Assertions.assertEquals("balance.txt", service.extractName("work/life/balance.txt"));
        Assertions.assertEquals("life", service.extractName("work/life/"));
        Assertions.assertEquals("file.txt", service.extractName("file.txt"));
    }

    @Test
    public void extractParentPath_returnsRelativeParentPath() {
        Assertions.assertEquals("", service.extractParentPath(""));
        Assertions.assertEquals("work/life/", service.extractParentPath("work/life/balance.txt"));
        Assertions.assertEquals("work/", service.extractParentPath("work/life/"));
        Assertions.assertEquals("", service.extractParentPath("file.txt"));
    }

    @Test
    public void normalizePath_keepsRootAndResourceShape() {
        Assertions.assertEquals("", service.normalizePath("", ResourceType.DIRECTORY));
        Assertions.assertEquals("docs/", service.normalizePath("docs", ResourceType.DIRECTORY));
        Assertions.assertEquals("docs/report.txt", service.normalizePath("docs/report.txt", ResourceType.FILE));
        Assertions.assertEquals("docs/report.txt", service.normalizeUploadRelativePath("docs\\report.txt"));
    }

    @Test
    public void normalizePath_rejectsInvalidSegments() {
        Assertions.assertThrows(IllegalArgumentException.class,
                () -> service.normalizePath("/docs/report.txt", ResourceType.FILE));
        Assertions.assertThrows(IllegalArgumentException.class,
                () -> service.normalizePath("docs//report.txt", ResourceType.FILE));
        Assertions.assertThrows(IllegalArgumentException.class,
                () -> service.normalizePath("docs/./report.txt", ResourceType.FILE));
        Assertions.assertThrows(IllegalArgumentException.class,
                () -> service.normalizePath("docs/../report.txt", ResourceType.FILE));
        Assertions.assertThrows(IllegalArgumentException.class,
                () -> service.normalizePath("docs/", ResourceType.FILE));
    }

    @Test
    public void normalizePath_rejectsBlankOnlyNames() {
        IllegalArgumentException blankPath = Assertions.assertThrows(IllegalArgumentException.class,
                () -> service.normalizePath(" ", ResourceType.FILE));
        Assertions.assertEquals("Path must not be blank", blankPath.getMessage());

        IllegalArgumentException blankSegment = Assertions.assertThrows(IllegalArgumentException.class,
                () -> service.normalizePath("docs/ /file.txt", ResourceType.FILE));
        Assertions.assertEquals("Path must not contain blank names", blankSegment.getMessage());
    }
}
