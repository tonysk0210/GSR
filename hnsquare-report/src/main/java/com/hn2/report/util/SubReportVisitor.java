package com.hn2.report.util;

import lombok.extern.slf4j.Slf4j;
import net.sf.jasperreports.crosstabs.JRCrosstab;
import net.sf.jasperreports.engine.*;
import net.sf.jasperreports.engine.design.JasperDesign;
import net.sf.jasperreports.engine.util.JRElementsVisitor;
import net.sf.jasperreports.engine.util.JRSaver;
import net.sf.jasperreports.engine.util.JRVisitorSupport;
import net.sf.jasperreports.engine.xml.JRXmlLoader;

import java.util.ArrayList;
import java.util.StringTokenizer;

@Slf4j
public class SubReportVisitor extends JRVisitorSupport {
    private final java.net.URI jasperPath;
    private ArrayList<String> completedSubReports = new ArrayList<String>(30);
    private Throwable subReportException = null;

    public SubReportVisitor(java.net.URI jasperPath) {
        this.jasperPath = jasperPath;
    }

    @Override
    public void visitSubreport(JRSubreport subreport) {

        try {
            String expression = subreport.getExpression().getText().replace(".jasper", ".jrxml");
            StringTokenizer st = new StringTokenizer(expression, "\"/");
            String subreportName = null;
            while (st.hasMoreTokens()) subreportName = st.nextToken();
            compileReport(subreportName);

        } catch (Throwable throwable) {
            throw new IllegalStateException("can't complie file. ", throwable);
        }
    }

    /** Recursively compile report and subreports */
    private JasperReport compileReport(String reportName) throws Throwable {
        JasperDesign jasperDesign = JRXmlLoader.load(jasperPath + reportName);
        JasperReport jasperReport = JasperCompileManager.compileReport(jasperDesign);
        JRSaver.saveObject(jasperReport, jasperPath + reportName.replace(".jrxml", ".jasper"));
        log.info("Saving compiled report to:" + jasperPath + reportName.replace(".jrxml", ".jasper"));
        // Compile sub reports
        JRElementsVisitor.visitReport(
                jasperReport,
                new JRVisitor() {
                    @Override
                    public void visitBreak(JRBreak breakElement) {}

                    @Override
                    public void visitChart(JRChart chart) {}

                    @Override
                    public void visitCrosstab(JRCrosstab crosstab) {}

                    @Override
                    public void visitElementGroup(JRElementGroup elementGroup) {}

                    @Override
                    public void visitEllipse(JREllipse ellipse) {}

                    @Override
                    public void visitFrame(JRFrame frame) {}

                    @Override
                    public void visitImage(JRImage image) {}

                    @Override
                    public void visitLine(JRLine line) {}

                    @Override
                    public void visitRectangle(JRRectangle rectangle) {}

                    @Override
                    public void visitStaticText(JRStaticText staticText) {}

                    @Override
                    public void visitSubreport(JRSubreport subreport) {
                        try {
                            String expression = subreport.getExpression().getText().replace(".jasper", "");
                            StringTokenizer st = new StringTokenizer(expression, "\"/");
                            String subReportName = null;
                            while (st.hasMoreTokens()) subReportName = st.nextToken();
                            // Sometimes the same subreport can be used multiple times, but
                            // there is no need to compile multiple times
                            if (completedSubReports.contains(subReportName)) return;
                            completedSubReports.add(subReportName);
                            compileReport(subReportName);
                        } catch (Throwable e) {
                            subReportException = e;
                        }
                    }

                    @Override
                    public void visitTextField(JRTextField textField) {}

                    @Override
                    public void visitComponentElement(JRComponentElement componentElement) {}

                    @Override
                    public void visitGenericElement(JRGenericElement element) {}
                });
        if (subReportException != null) throw new RuntimeException(subReportException);
        return jasperReport;
    }
}
