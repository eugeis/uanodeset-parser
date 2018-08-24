package com.digitalpetri.opcua.nodeset.util;

import org.eclipse.milo.opcua.stack.core.Identifiers;
import org.eclipse.milo.opcua.stack.core.serialization.OpcUaXmlStreamDecoder;
import org.eclipse.milo.opcua.stack.core.types.builtin.DataValue;
import org.eclipse.milo.opcua.stack.core.types.builtin.NodeId;
import org.eclipse.milo.opcua.stack.core.types.builtin.Variant;
import org.eclipse.milo.opcua.stack.core.types.builtin.unsigned.UInteger;
import org.opcfoundation.ua.generated.GeneratedReference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.xml.bind.JAXBElement;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import java.io.StringReader;
import java.io.StringWriter;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.eclipse.milo.opcua.stack.core.types.builtin.unsigned.Unsigned.uint;

public class AttributeUtil {

    private static final Logger LOGGER = LoggerFactory.getLogger(AttributeUtil.class);

    public static NodeId parseDataType(String dataTypeString, Map<String, NodeId> aliases) {
        try {
            return NodeId.parse(dataTypeString);
        } catch (Throwable t) {
            if (aliases.containsKey(dataTypeString)) {
                return aliases.get(dataTypeString);
            } else {
                // Ok, last effort...
                Optional<NodeId> nodeId = Arrays.stream(Identifiers.class.getFields())
                    .filter(field -> field.getName().equals(dataTypeString))
                    .findFirst()
                    .map(field -> {
                        try {
                            return (NodeId) field.get(null);
                        } catch (Throwable ex) {
                            throw new RuntimeException("Couldn't get ReferenceTypeId for " + dataTypeString, ex);
                        }
                    });

                return nodeId.orElseThrow(RuntimeException::new);
            }
        }
    }

    public static NodeId parseReferenceTypeId(GeneratedReference gReference, Map<String, NodeId> aliases) {
        String referenceType = gReference.getReferenceType();

        try {
            return NodeId.parse(referenceType);
        } catch (Throwable t) {
            if (aliases.containsKey(referenceType)) {
                return aliases.get(referenceType);
            } else {
                // Ok, last effort...
                Optional<NodeId> nodeId = Arrays.stream(Identifiers.class.getFields())
                    .filter(field -> field.getName().equals(referenceType))
                    .findFirst()
                    .map(field -> {
                        try {
                            return (NodeId) field.get(null);
                        } catch (Throwable ex) {
                            throw new RuntimeException("Couldn't get ReferenceTypeId for " + referenceType, ex);
                        }
                    });

                return nodeId.orElseThrow(RuntimeException::new);
            }
        }
    }

    public static DataValue parseValue(Object value, Marshaller marshaller) {
        JAXBElement<?> jaxbElement = JAXBElement.class.cast(value);

        StringWriter sw = new StringWriter();

        try {
            marshaller.marshal(jaxbElement, sw);
        } catch (JAXBException e) {
            LOGGER.warn("unable to marshal JAXB element: " + jaxbElement, e);
            return new DataValue(Variant.NULL_VALUE);
        }

        String xmlString = sw.toString();
        try {
            OpcUaXmlStreamDecoder xmlDecoder = new OpcUaXmlStreamDecoder(new StringReader(xmlString));
            Object valueObject = xmlDecoder.readVariantValue();

            return new DataValue(new Variant(valueObject));
        } catch (Throwable t) {
            LOGGER.warn("unable to parse Value: " + xmlString, t);
            return new DataValue(Variant.NULL_VALUE);
        }
    }

    public static UInteger[] parseArrayDimensions(List<String> list) {
        if (list.isEmpty()) {
            return new UInteger[0];
        } else {
            String[] ss = list.get(0).split(",");
            UInteger[] dimensions = new UInteger[ss.length];

            for (int i = 0; i < ss.length; i++) {
                dimensions[i] = uint(Integer.parseInt(ss[i]));
            }

            return dimensions;
        }
    }

}
