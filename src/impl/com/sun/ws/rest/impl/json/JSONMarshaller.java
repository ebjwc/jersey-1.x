/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 * 
 * Copyright 2007 Sun Microsystems, Inc. All rights reserved. 
 * 
 * The contents of this file are subject to the terms of the Common Development
 * and Distribution License("CDDL") (the "License").  You may not use this file
 * except in compliance with the License. 
 * 
 * You can obtain a copy of the License at:
 *     https://jersey.dev.java.net/license.txt
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * When distributing the Covered Code, include this CDDL Header Notice in each
 * file and include the License file at:
 *     https://jersey.dev.java.net/license.txt
 * If applicable, add the following below this CDDL Header, with the fields
 * enclosed by brackets [] replaced by your own identifying information:
 *     "Portions Copyrighted [year] [name of copyright owner]"
 */
package com.sun.ws.rest.impl.json;

import com.sun.ws.rest.impl.json.writer.JsonXmlStreamWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.util.Collection;
import java.util.Map;
import java.util.Map.Entry;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.bind.PropertyException;
import javax.xml.bind.ValidationEventHandler;
import javax.xml.bind.annotation.adapters.XmlAdapter;
import javax.xml.bind.attachment.AttachmentMarshaller;
import javax.xml.stream.XMLEventWriter;
import javax.xml.stream.XMLStreamWriter;
import javax.xml.transform.Result;
import javax.xml.validation.Schema;
import org.codehaus.jettison.badgerfish.BadgerFishXMLStreamWriter;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.mapped.Configuration;
import org.codehaus.jettison.mapped.MappedNamespaceConvention;
import org.codehaus.jettison.mapped.MappedXMLStreamWriter;
import org.w3c.dom.Node;
import org.xml.sax.ContentHandler;

/**
 *
 * @author japod
 */
public class JSONMarshaller implements Marshaller {

    private JAXBContext jaxbContext;
    private Marshaller jaxbMarshaller;
    private JSONJAXBContext.JSONNotation jsonNotation;
    private boolean jsonEnabled;
    private boolean jsonRootUnwrapping;
    private Collection<String> arrays;
    private Collection<String> nonStrings;
    private Map<String, String> xml2jsonNamespace;

    public JSONMarshaller(JAXBContext jaxbContext, Map<String, Object> properties) throws JAXBException {
        try {
            this.jaxbContext = jaxbContext;
            this.jaxbMarshaller = jaxbContext.createMarshaller();
            setProperties(properties);
        } catch (PropertyException ex) {
            Logger.getLogger(JSONMarshaller.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    public void marshal(Object jaxbObject, Result result) throws JAXBException {
        jaxbMarshaller.marshal(jaxbObject, result);
    }

    public void marshal(Object jaxbObject, OutputStream os) throws JAXBException {
        if (jsonEnabled) {
            jaxbMarshaller.marshal(jaxbObject, createXmlStreamWriter(new OutputStreamWriter(os)));
        } else {
            jaxbMarshaller.marshal(jaxbObject, os);
        }
    }

    public void marshal(Object jaxbObject, File file) throws JAXBException {
        if (jsonEnabled) {
            try {
                jaxbMarshaller.marshal(jaxbObject, createXmlStreamWriter(new FileWriter(file)));
            } catch (IOException ex) {
                Logger.getLogger(JSONMarshaller.class.getName()).log(
                        Level.SEVERE, "IOException caught when marshalling into a file.", ex);
                throw new JAXBException(ex.getMessage(), ex);
            }
        } else {
            jaxbMarshaller.marshal(jaxbObject, file);
        }
    }

    public void marshal(Object jaxbObject, Writer writer) throws JAXBException {
        if (jsonEnabled) {
            jaxbMarshaller.marshal(jaxbObject, createXmlStreamWriter(writer));
        } else {
            jaxbMarshaller.marshal(jaxbObject, writer);
        }
    }

    public void marshal(Object jaxbObject, ContentHandler handler) throws JAXBException {
        jaxbMarshaller.marshal(jaxbObject, handler);
    }

    public void marshal(Object jaxbObject, Node node) throws JAXBException {
        jaxbMarshaller.marshal(jaxbObject, node);
    }

    public void marshal(Object jaxbObject, XMLStreamWriter writer) throws JAXBException {
        jaxbMarshaller.marshal(jaxbObject, writer);
    }

    public void marshal(Object jaxbObject, XMLEventWriter writer) throws JAXBException {
        jaxbMarshaller.marshal(jaxbObject, writer);
    }

    public Node getNode(Object jaxbObject) throws JAXBException {
        return jaxbMarshaller.getNode(jaxbObject);
    }

    public void setProperty(String key, Object value) throws PropertyException {
        if (JSONJAXBContext.JSON_ENABLED.equals(key)) {
            this.jsonEnabled = (Boolean) value;
        } else if (JSONJAXBContext.JSON_NOTATION.equals(key)) {
            this.jsonNotation = JSONJAXBContext.JSONNotation.valueOf((String) value);
        } else if (JSONJAXBContext.JSON_ROOT_UNWRAPPING.equals(key)) {
            this.jsonRootUnwrapping = (Boolean) value;
        } else if (JSONJAXBContext.JSON_ARRAYS.equals(key)) {
            try {
                this.arrays = JSONJAXBContext.asCollection((String) value);
            } catch (JSONException e) {
                throw new PropertyException("JSON exception when trying to set " + JSONJAXBContext.JSON_ARRAYS + " property.", e);
            }
        } else if (JSONJAXBContext.JSON_NON_STRINGS.equals(key)) {
            try {
                this.nonStrings = JSONJAXBContext.asCollection((String) value);
            } catch (JSONException e) {
                throw new PropertyException("JSON exception when trying to set " + JSONJAXBContext.JSON_NON_STRINGS + " property.", e);
            }
        } else if (JSONJAXBContext.JSON_XML2JSON_NS.equals(key)) {
            try {
                this.xml2jsonNamespace = JSONJAXBContext.asMap((String) value);
            } catch (JSONException e) {
                throw new PropertyException("JSON exception when trying to set " + JSONJAXBContext.JSON_XML2JSON_NS + " property.", e);
            }
        } else {
            if (!key.startsWith(JSONJAXBContext.NAMESPACE)) {
                jaxbMarshaller.setProperty(key, value);
            }
        }
    }

    
    public Object getProperty(String key) throws PropertyException {
        if (JSONJAXBContext.JSON_ENABLED.equals(key)) {
            return this.jsonEnabled;
        } else if (JSONJAXBContext.JSON_NOTATION.equals(key)) {
            return this.jsonNotation.name();
        } else if (JSONJAXBContext.JSON_ROOT_UNWRAPPING.equals(key)) {
            return this.jsonRootUnwrapping;
        } else if (JSONJAXBContext.JSON_ARRAYS.equals(key)) {
            return JSONJAXBContext.asJsonArray(this.arrays);
        } else if (JSONJAXBContext.JSON_NON_STRINGS.equals(key)) {
            return JSONJAXBContext.asJsonArray(this.nonStrings);
        } else if (JSONJAXBContext.JSON_XML2JSON_NS.equals(key)) {
            return JSONJAXBContext.asJsonObject(this.xml2jsonNamespace);
        } else {
            if (key.startsWith(JSONJAXBContext.NAMESPACE)) {
                return null;
            } else {
                return jaxbMarshaller.getProperty(key);
            }
        }
    }

    public void setEventHandler(ValidationEventHandler handler) throws JAXBException {
        jaxbMarshaller.setEventHandler(handler);
    }

    public ValidationEventHandler getEventHandler() throws JAXBException {
        return jaxbMarshaller.getEventHandler();
    }

    public void setAdapter(XmlAdapter adapter) {
        jaxbMarshaller.setAdapter(adapter);
    }

    public <A extends XmlAdapter> void setAdapter(Class<A> type, A adapter) {
        jaxbMarshaller.setAdapter(type, adapter);
    }

    public <A extends XmlAdapter> A getAdapter(Class<A> type) {
        return jaxbMarshaller.getAdapter(type);
    }

    public void setAttachmentMarshaller(AttachmentMarshaller marshaller) {
        jaxbMarshaller.setAttachmentMarshaller(marshaller);
    }

    public AttachmentMarshaller getAttachmentMarshaller() {
        return jaxbMarshaller.getAttachmentMarshaller();
    }

    public void setSchema(Schema schema) {
        jaxbMarshaller.setSchema(schema);
    }

    public Schema getSchema() {
        return jaxbMarshaller.getSchema();
    }

    public void setListener(Listener listener) {
        jaxbMarshaller.setListener(listener);
    }

    public Listener getListener() {
        return jaxbMarshaller.getListener();
    }

    private void setProperties(Map<String, Object> properties) throws PropertyException {
        if (null != properties) {
            for (Entry<String, Object> entry : properties.entrySet()) {
                setProperty(entry.getKey(), entry.getValue());
            }
        }
    }

    XMLStreamWriter createXmlStreamWriter(Writer writer) {
        XMLStreamWriter xmlStreamWriter;
        if (JSONJAXBContext.JSONNotation.MAPPED == this.jsonNotation) {
            xmlStreamWriter = new JsonXmlStreamWriter(writer, this.jsonRootUnwrapping, this.arrays, this.nonStrings);
        } else if (JSONJAXBContext.JSONNotation.MAPPED_JETTISON == this.jsonNotation) {
                Configuration jmConfig;
                if (null == this.xml2jsonNamespace) {
                    jmConfig = new Configuration();
                } else {
                    jmConfig = new Configuration(this.xml2jsonNamespace);
                }
            xmlStreamWriter = new MappedXMLStreamWriter(
                    new MappedNamespaceConvention(jmConfig), writer);
        } else {
            xmlStreamWriter = new BadgerFishXMLStreamWriter(writer);
        }
        return xmlStreamWriter;
    }
}
