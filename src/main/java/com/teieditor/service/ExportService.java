package com.teieditor.service;

import net.sf.saxon.TransformerFactoryImpl;
import org.apache.fop.apps.Fop;
import org.apache.fop.apps.FopFactory;
import org.apache.fop.apps.MimeConstants;

import javax.xml.transform.Result;
import javax.xml.transform.Source;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.sax.SAXResult;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;
import java.io.*;

public class ExportService {

    private final TransformerFactory transformerFactory;
    private final FopFactory fopFactory;

    public ExportService() {
        this.transformerFactory = new TransformerFactoryImpl();
        this.fopFactory = FopFactory.newInstance(new File(".").toURI());
    }

    public void transform(String xmlContent, File xsltFile, File outputFile) throws Exception {
        if (xsltFile == null || !xsltFile.exists()) {
            throw new FileNotFoundException("XSLT Stylesheet not found.");
        }

        Source xmlSource = new StreamSource(new StringReader(xmlContent));
        Source xsltSource = new StreamSource(xsltFile);
        
        Transformer transformer = transformerFactory.newTransformer(xsltSource);
        
        try (FileOutputStream out = new FileOutputStream(outputFile)) {
            StreamResult result = new StreamResult(out);
            transformer.transform(xmlSource, result);
        }
    }

    public void transformToPdf(String xmlContent, File xsltFoFile, File outputFile) throws Exception {
        if (xsltFoFile == null || !xsltFoFile.exists()) {
            throw new FileNotFoundException("PDF XSLT (FO) not found.");
        }

        Source xmlSource = new StreamSource(new StringReader(xmlContent));
        Source xsltSource = new StreamSource(xsltFoFile);

        try (OutputStream out = new BufferedOutputStream(new FileOutputStream(outputFile))) {
            Fop fop = fopFactory.newFop(MimeConstants.MIME_PDF, out);
            Transformer transformer = transformerFactory.newTransformer(xsltSource);
            Result res = new SAXResult(fop.getDefaultHandler());
            transformer.transform(xmlSource, res);
        }
    }
}