package com.hn2.report.util;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ResourceLoader;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.util.Objects;

/**
 * JasperReport Environment
 *
 * @author hsien
 */
@Component
public class ReportEnvironment {

    protected int marginTop = 0;
    protected int marginBottom = 0;
    protected int marginLeft = 0;
    protected int marginRight = 0;
    protected boolean addMargin = false;
    private InputStream jrxmlFile;
    private URI jrxmlDir;
    private String reportFormat;
    private boolean customMargin = false;
    /** 資源物件 */
    @Autowired private ResourceLoader resourceLoader;

    public boolean isCustomMargin() {
        return customMargin;
    }

    public InputStream getJrxmlFile() {
        return jrxmlFile;
    }

    public URI getJrxmlDir() {
        return jrxmlDir;
    }

    public String getReportFormat() {
        return reportFormat;
    }

    private final static String REPORT_ROOT_PATH = "jasperreports/";

    /**
     * 設定 jrxml
     *
     * @param jrxmlPath
     * @throws IOException
     */
    public void setFile(String jrxmlPath) throws IOException {
        String[] paths = jrxmlPath.split("/");
        String folderPath = REPORT_ROOT_PATH;
        for (int i = 0; i <= paths.length - 2; i++) {
            folderPath += paths[i].concat("/");
        }
        jrxmlDir = resourceLoader.getResource("classpath:" + folderPath).getURI();
        jrxmlFile = Objects.requireNonNull(resourceLoader.getClassLoader()).getResourceAsStream(REPORT_ROOT_PATH+jrxmlPath);
    }

    public void setFormat(String format) {
        reportFormat = format;
    }

    /**
     * 設定邊界
     *
     * @param top
     * @param bottom
     * @param left
     * @param right
     * @param addMargin 是否累加原報表的邊界
     */
    public void setMarginSize(int top, int bottom, int left, int right, boolean addMargin) {
        marginTop = top;
        marginBottom = bottom;
        marginLeft = left;
        marginRight = right;
        this.addMargin = addMargin;
        customMargin = true;
    }
}
