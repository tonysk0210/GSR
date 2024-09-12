package com.hn2.report.util;

import net.sf.jasperreports.engine.*;
import net.sf.jasperreports.engine.data.JRBeanCollectionDataSource;
import net.sf.jasperreports.engine.export.JRPdfExporter;
import net.sf.jasperreports.engine.export.oasis.JROdsExporter;
import net.sf.jasperreports.engine.export.ooxml.JRDocxExporter;
import net.sf.jasperreports.engine.export.ooxml.JRXlsxExporter;
import net.sf.jasperreports.engine.util.JRElementsVisitor;
import net.sf.jasperreports.export.*;
import org.springframework.stereotype.Component;

import java.io.ByteArrayOutputStream;
import java.util.Collection;
import java.util.Map;

/**
 * JasperReport Generator
 *
 * @author hsien
 */
@Component
public class ReportGenerator {

    /**
     * 產生報表檔案
     *
     * @param reportEnvironment 報表環境
     * @param data 報表資料
     * @return byte[] 報表檔案
     * @throws Exception Exception
     */
    public byte[] generate(ReportEnvironment reportEnvironment, Collection<?> data)
            throws Exception {
        return generate(reportEnvironment, data, null);
    }

    /**
     * 產生報表檔案
     *
     * @param reportEnvironment 報表環境
     * @param data 報表資料
     * @param parameters 報表參數
     * @return byte[] 報表檔案
     * @throws Exception Exception
     */
    public byte[] generate(
            ReportEnvironment reportEnvironment, Collection<?> data, Map<String, Object> parameters)
            throws Exception {
        JasperPrint jasperPrint = getJasperPrint(reportEnvironment, data, parameters);

        // 自定邊界
        if (reportEnvironment.isCustomMargin()) {

            int finalTop = reportEnvironment.marginTop;
            int finalRight = reportEnvironment.marginRight;
            int finalBottom = reportEnvironment.marginBottom;
            int finalLeft = reportEnvironment.marginLeft;

            if (reportEnvironment.addMargin) {
                finalTop += jasperPrint.getTopMargin();
                finalRight += jasperPrint.getRightMargin();
                finalBottom += jasperPrint.getBottomMargin();
                finalLeft += jasperPrint.getLeftMargin();
            }

            jasperPrint.setTopMargin(finalTop);
            jasperPrint.setRightMargin(finalRight);
            jasperPrint.setBottomMargin(finalBottom);
            jasperPrint.setLeftMargin(finalLeft);
        }

        ByteArrayOutputStream oStream = new ByteArrayOutputStream();
        byte[] bytes = null;

        switch (reportEnvironment.getReportFormat()) {
            case ReportFormat.FORMAT_PDF:
                bytes = generatePdf(jasperPrint, oStream);
                break;
            case ReportFormat.FORMAT_ODS:
                bytes = generateOds(jasperPrint, oStream);
                break;
            case ReportFormat.FORMAT_XLSX:
                bytes = generateXlsx(jasperPrint, oStream);
                break;
            case ReportFormat.FORMAT_DOCX:
                bytes = generateDocx(jasperPrint, oStream);
                break;
            default:
        }
        return bytes;
    }

    /**
     * 產製PDF
     *
     * @param jasperPrint JasperPrint
     * @param oStream ByteArrayOutputStream
     * @return byte[] 報表檔金
     * @throws Exception Exception
     */
    private byte[] generatePdf(JasperPrint jasperPrint, ByteArrayOutputStream oStream)
            throws Exception {
        SimplePdfReportConfiguration configuration = new SimplePdfReportConfiguration();
        JRPdfExporter exporter = new JRPdfExporter();
        exporter.setExporterInput(new SimpleExporterInput(jasperPrint));
        exporter.setExporterOutput(new SimpleOutputStreamExporterOutput(oStream));
        exporter.setConfiguration(configuration);
        exporter.exportReport();
        oStream.flush();
        return oStream.toByteArray();
    }

    /**
     * 產製ODS
     *
     * @param jasperPrint JasperPrint
     * @param oStream ByteArrayOutputStream
     * @return byte[] 報表檔
     * @throws Exception Exception
     */
    private byte[] generateOds(JasperPrint jasperPrint, ByteArrayOutputStream oStream)
            throws Exception {
        SimpleOdsReportConfiguration configuration = new SimpleOdsReportConfiguration();
        configuration.setDetectCellType(true);
        configuration.setOnePagePerSheet(true);
        configuration.setIgnoreGraphics(false);
        JROdsExporter exporter = new JROdsExporter();
        exporter.setExporterInput(new SimpleExporterInput(jasperPrint));
        exporter.setExporterOutput(new SimpleOutputStreamExporterOutput(oStream));
        exporter.setConfiguration(configuration);
        exporter.exportReport();
        return oStream.toByteArray();
    }

    /**
     * 產製XLSX
     *
     * @param jasperPrint JasperPrint
     * @param oStream ByteArrayOutputStream
     * @return byte[] 報表檔
     * @throws Exception Exception
     */
    private byte[] generateXlsx(JasperPrint jasperPrint, ByteArrayOutputStream oStream)
            throws Exception {
        SimpleXlsxReportConfiguration configuration = new SimpleXlsxReportConfiguration();
        configuration.setDetectCellType(true);
        configuration.setOnePagePerSheet(true);
        configuration.setIgnoreGraphics(false);
        JRXlsxExporter exporter = new JRXlsxExporter();
        exporter.setExporterInput(new SimpleExporterInput(jasperPrint));
        exporter.setExporterOutput(new SimpleOutputStreamExporterOutput(oStream));
        exporter.setConfiguration(configuration);
        exporter.exportReport();
        return oStream.toByteArray();
    }

    /**
     * 產製DOCX
     *
     * @param jasperPrint JasperPrint
     * @param oStream ByteArrayOutputStream
     * @return byte[] 報表檔
     * @throws Exception Exception
     */
    private byte[] generateDocx(JasperPrint jasperPrint, ByteArrayOutputStream oStream)
            throws Exception {
        SimpleDocxReportConfiguration configuration = new SimpleDocxReportConfiguration();
        JRDocxExporter exporter = new JRDocxExporter();
        exporter.setExporterInput(new SimpleExporterInput(jasperPrint));
        exporter.setExporterOutput(new SimpleOutputStreamExporterOutput(oStream));
        exporter.setConfiguration(configuration);
        exporter.exportReport();
        return oStream.toByteArray();
    }

    /**
     * 取 JasperPrint
     *
     * @param reportEnvironment 報表環境
     * @param data 報表資料
     * @param parameters 報表參數
     * @return JasperPrint
     * @throws Exception Exception
     */
    private JasperPrint getJasperPrint(
            ReportEnvironment reportEnvironment, Collection<?> data, Map<String, Object> parameters)
            throws Exception {

        JasperReport jasperReport =
                JasperCompileManager.compileReport(reportEnvironment.getJrxmlFile());

        JRElementsVisitor.visitReport(
                jasperReport, new SubReportVisitor(reportEnvironment.getJrxmlDir()));

        JRDataSource jrDataSource = new JRBeanCollectionDataSource(data);
        return JasperFillManager.fillReport(jasperReport, parameters, jrDataSource);
    }
}