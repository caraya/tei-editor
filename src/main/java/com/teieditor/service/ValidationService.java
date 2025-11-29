package com.teieditor.service;

import com.thaiopensource.util.PropertyMapBuilder;
import com.thaiopensource.validate.ValidateProperty;
import com.thaiopensource.validate.ValidationDriver;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

import java.io.IOException;
import java.io.StringReader;
import java.net.URL;

public class ValidationService {

    public boolean validateTei(String xmlContent) {
        // 1. Create the PropertyMapBuilder
        PropertyMapBuilder properties = new PropertyMapBuilder();
        
        // FIX: Use the builder's put method instead of the PropertyId's put method
        // Old (Deprecated): ValidateProperty.ERROR_HANDLER.put(properties, new DefaultHandler());
        // New (Correct):
        properties.put(ValidateProperty.ERROR_HANDLER, new DefaultHandler());
        
        // 2. Initialize the Driver with the map
        ValidationDriver driver = new ValidationDriver(properties.toPropertyMap());

        try {
            // 3. Load Schema
            // Ensure you have src/main/resources/schema/tei_all.rng
            URL schemaUrl = getClass().getResource("/schema/tei_all.rng");
            
            if (schemaUrl == null) {
                System.err.println("CRITICAL ERROR: 'schema/tei_all.rng' not found in resources.");
                return false;
            }

            InputSource schemaSource = new InputSource(schemaUrl.openStream());
            
            // Load the schema into the driver
            if (!driver.loadSchema(schemaSource)) {
                System.err.println("Error loading schema definition. Check if tei_all.rng is valid.");
                return false;
            }

            // 4. Validate the XML content
            InputSource xmlSource = new InputSource(new StringReader(xmlContent));
            return driver.validate(xmlSource);

        } catch (IOException | SAXException e) {
            e.printStackTrace();
            return false;
        }
    }
}