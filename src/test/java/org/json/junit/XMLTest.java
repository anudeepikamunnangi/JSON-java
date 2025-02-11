package org.json.junit;

/*
Public Domain.
*/

import static org.junit.jupiter.api.Assertions.*;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.StringReader;
import java.nio.charset.Charset;
import java.util.HashMap;
import java.util.Map;

import org.json.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;


/**
 * Tests for JSON-Java XML.java
 * Note: noSpace() will be tested by JSONMLTest
 */
public class XMLTest {
    /**
     * JUnit supports temporary files and folders that are cleaned up after the test.
     * https://garygregory.wordpress.com/2010/01/20/junit-tip-use-rules-to-manage-temporary-files-and-folders/ 
     */
    @TempDir
    public File testFolder;


    /**
     * JSONObject from a null XML string.
     * Expects a NullPointerException
     */
    @Test
    void shouldHandleNullXML() {
        assertThrows(NullPointerException.class, () -> {
            String xmlStr = null;
            JSONObject jsonObject = XML.toJSONObject(xmlStr);
            assertTrue(jsonObject.isEmpty(), "jsonObject should be empty");
        });
    }

    /**
     * Empty JSONObject from an empty XML string.
     */
    @Test
    void shouldHandleEmptyXML() {

        String xmlStr = "";
        JSONObject jsonObject = XML.toJSONObject(xmlStr);
        assertTrue(jsonObject.isEmpty(), "jsonObject should be empty");
    }

    /**
     * Empty JSONObject from a non-XML string.
     */
    @Test
    void shouldHandleNonXML() {
        String xmlStr = "{ \"this is\": \"not xml\"}";
        JSONObject jsonObject = XML.toJSONObject(xmlStr);
        assertTrue(jsonObject.isEmpty(), "xml string should be empty");
    }

    /**
     * Invalid XML string (tag contains a frontslash).
     * Expects a JSONException
     */
    @Test
    void shouldHandleInvalidSlashInTag() {
        String xmlStr = 
            "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n"+
            "<addresses xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\""+
            "   xsi:noNamespaceSchemaLocation='test.xsd'>\n"+
            "    <address>\n"+
            "       <name/x>\n"+
            "       <street>abc street</street>\n"+
            "   </address>\n"+
            "</addresses>";
        try {
            XML.toJSONObject(xmlStr);
            fail("Expecting a JSONException");
        } catch (JSONException e) {
            assertEquals("Misshaped tag at 176 [character 14 line 4]",
                    e.getMessage(),
                    "Expecting an exception message");
        }
    }

    /**
     * Invalid XML string ('!' char in tag)
     * Expects a JSONException
     */
    @Test
    void shouldHandleInvalidBangInTag() {
        String xmlStr = 
            "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n"+
            "<addresses xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\""+
            "   xsi:noNamespaceSchemaLocation='test.xsd'>\n"+
            "    <address>\n"+
            "       <name/>\n"+
            "       <!>\n"+
            "   </address>\n"+
            "</addresses>";
        try {
            XML.toJSONObject(xmlStr);
            fail("Expecting a JSONException");
        } catch (JSONException e) {
            assertEquals("Misshaped meta tag at 214 [character 12 line 7]",
                    e.getMessage(),
                    "Expecting an exception message");
        }
    }

    /**
     * Invalid XML string ('!' char and no closing tag brace)
     * Expects a JSONException
     */
    @Test
    void shouldHandleInvalidBangNoCloseInTag() {
        String xmlStr = 
            "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n"+
            "<addresses xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\""+
            "   xsi:noNamespaceSchemaLocation='test.xsd'>\n"+
            "    <address>\n"+
            "       <name/>\n"+
            "       <!\n"+
            "   </address>\n"+
            "</addresses>";
        try {
            XML.toJSONObject(xmlStr);
            fail("Expecting a JSONException");
        } catch (JSONException e) {
            assertEquals("Misshaped meta tag at 213 [character 12 line 7]",
                    e.getMessage(),
                    "Expecting an exception message");
        }
    }

    /**
     * Invalid XML string (no end brace for tag)
     * Expects JSONException
     */
    @Test
    void shouldHandleNoCloseStartTag() {
        String xmlStr = 
            "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n"+
            "<addresses xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\""+
            "   xsi:noNamespaceSchemaLocation='test.xsd'>\n"+
            "    <address>\n"+
            "       <name/>\n"+
            "       <abc\n"+
            "   </address>\n"+
            "</addresses>";
        try {
            XML.toJSONObject(xmlStr);
            fail("Expecting a JSONException");
        } catch (JSONException e) {
            assertEquals("Misplaced '<' at 193 [character 4 line 6]",
                    e.getMessage(),
                    "Expecting an exception message");
        }
    }

    /**
     * Invalid XML string (partial CDATA chars in tag name)
     * Expects JSONException
     */
    @Test
    void shouldHandleInvalidCDATABangInTag() {
        String xmlStr = 
            "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n"+
            "<addresses xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\""+
            "   xsi:noNamespaceSchemaLocation='test.xsd'>\n"+
            "    <address>\n"+
            "       <name>Joe Tester</name>\n"+
            "       <![[]>\n"+
            "   </address>\n"+
            "</addresses>";
        try {
            XML.toJSONObject(xmlStr);
            fail("Expecting a JSONException");
        } catch (JSONException e) {
            assertEquals("Expected 'CDATA[' at 204 [character 11 line 5]",
                    e.getMessage(),
                    "Expecting an exception message");
        }
    }

    /**
     * Null JSONObject in XML.toString()
     */
    @Test
    void shouldHandleNullJSONXML() {
        JSONObject jsonObject= null;
        String actualXml=XML.toString(jsonObject);
        assertEquals("\"null\"",actualXml,"generated XML does not equal expected XML");
    }

    /**
     * Empty JSONObject in XML.toString()
     */
    @Test
    void shouldHandleEmptyJSONXML() {
        JSONObject jsonObject= new JSONObject();
        String xmlStr = XML.toString(jsonObject);
        assertTrue(xmlStr.isEmpty(), "xml string should be empty");
    }

    /**
     * No SML start tag. The ending tag ends up being treated as content.
     */
    @Test
    void shouldHandleNoStartTag() {
        String xmlStr = 
            "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n"+
            "<addresses xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\""+
            "   xsi:noNamespaceSchemaLocation='test.xsd'>\n"+
            "    <address>\n"+
            "       <name/>\n"+
            "       <nocontent/>>\n"+
            "   </address>\n"+
            "</addresses>";
        String expectedStr = 
            "{\"addresses\":{\"address\":{\"name\":\"\",\"nocontent\":\"\",\""+
            "content\":\">\"},\"xsi:noNamespaceSchemaLocation\":\"test.xsd\",\""+
            "xmlns:xsi\":\"http://www.w3.org/2001/XMLSchema-instance\"}}";
        JSONObject jsonObject = XML.toJSONObject(xmlStr);
        JSONObject expectedJsonObject = new JSONObject(expectedStr);
        Util.compareActualVsExpectedJsonObjects(jsonObject,expectedJsonObject);
    }

    /**
     * Valid XML to JSONObject
     */
    @Test
    void shouldHandleSimpleXML() {
        String xmlStr = 
            "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n"+
            "<addresses xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\""+
            "   xsi:noNamespaceSchemaLocation='test.xsd'>\n"+
            "   <address>\n"+
            "       <name>Joe Tester</name>\n"+
            "       <street>[CDATA[Baker street 5]</street>\n"+
            "       <NothingHere/>\n"+
            "       <TrueValue>true</TrueValue>\n"+
            "       <FalseValue>false</FalseValue>\n"+
            "       <NullValue>null</NullValue>\n"+
            "       <PositiveValue>42</PositiveValue>\n"+
            "       <NegativeValue>-23</NegativeValue>\n"+
            "       <DoubleValue>-23.45</DoubleValue>\n"+
            "       <Nan>-23x.45</Nan>\n"+
            "       <ArrayOfNum>1, 2, 3, 4.1, 5.2</ArrayOfNum>\n"+
            "   </address>\n"+
            "</addresses>";

        String expectedStr = 
            "{\"addresses\":{\"address\":{\"street\":\"[CDATA[Baker street 5]\","+
            "\"name\":\"Joe Tester\",\"NothingHere\":\"\",TrueValue:true,\n"+
            "\"FalseValue\":false,\"NullValue\":null,\"PositiveValue\":42,\n"+
            "\"NegativeValue\":-23,\"DoubleValue\":-23.45,\"Nan\":-23x.45,\n"+
            "\"ArrayOfNum\":\"1, 2, 3, 4.1, 5.2\"\n"+
            "},\"xsi:noNamespaceSchemaLocation\":"+
            "\"test.xsd\",\"xmlns:xsi\":\"http://www.w3.org/2001/"+
            "XMLSchema-instance\"}}";

        compareStringToJSONObject(xmlStr, expectedStr);
        compareReaderToJSONObject(xmlStr, expectedStr);
        compareFileToJSONObject(xmlStr, expectedStr);
    }

    /**
     * Tests to verify that supported escapes in XML are converted to actual values.
     */
    @Test
    void xmlEscapeToJson(){
        String xmlStr = 
            "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n"+
            "<root>"+
            "<rawQuote>\"</rawQuote>"+
            "<euro>A &#8364;33</euro>"+
            "<euroX>A &#x20ac;22&#x20AC;</euroX>"+
            "<unknown>some text &copy;</unknown>"+
            "<known>&#x0022; &quot; &amp; &apos; &lt; &gt;</known>"+
            "<high>&#x1D122; &#x10165;</high>" +
            "</root>";
        String expectedStr = 
            "{\"root\":{" +
            "\"rawQuote\":\"\\\"\"," +
            "\"euro\":\"A €33\"," +
            "\"euroX\":\"A €22€\"," +
            "\"unknown\":\"some text &copy;\"," +
            "\"known\":\"\\\" \\\" & ' < >\"," +
            "\"high\":\"𝄢 𐅥\""+
            "}}";
        
        compareStringToJSONObject(xmlStr, expectedStr);
        compareReaderToJSONObject(xmlStr, expectedStr);
        compareFileToJSONObject(xmlStr, expectedStr);
    }

    /**
     * Tests that control characters are escaped.
     */
    @Test
    void jsonToXmlEscape(){
        final String jsonSrc = "{\"amount\":\"10,00 €\","
                + "\"description\":\"Ação Válida\u0085\","
                + "\"xmlEntities\":\"\\\" ' & < >\""
                + "}";
        JSONObject json = new JSONObject(jsonSrc);
        String xml = XML.toString(json);
        //test control character not existing
        assertFalse(xml.contains("\u0085"), "Escaping \u0085 failed. Found in XML output.");
        assertTrue(xml.contains("&#x85;"), "Escaping \u0085 failed. Entity not found in XML output.");
        // test normal unicode existing
        assertTrue(xml.contains("€"), "Escaping € failed. Not found in XML output.");
        assertTrue(xml.contains("ç"), "Escaping ç failed. Not found in XML output.");
        assertTrue(xml.contains("ã"), "Escaping ã failed. Not found in XML output.");
        assertTrue(xml.contains("á"), "Escaping á failed. Not found in XML output.");
        // test XML Entities converted
        assertTrue(xml.contains("&quot;"), "Escaping \" failed. Not found in XML output.");
        assertTrue(xml.contains("&apos;"), "Escaping ' failed. Not found in XML output.");
        assertTrue(xml.contains("&amp;"), "Escaping & failed. Not found in XML output.");
        assertTrue(xml.contains("&lt;"), "Escaping < failed. Not found in XML output.");
        assertTrue(xml.contains("&gt;"), "Escaping > failed. Not found in XML output.");
    }

    /**
     * Valid XML with comments to JSONObject
     */
    @Test
    void shouldHandleCommentsInXML() {

        String xmlStr = 
                "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n"+
                "<!-- this is a comment -->\n"+
                "<addresses>\n"+
                "   <address>\n"+
                "       <![CDATA[ this is -- <another> comment ]]>\n"+
                "       <name>Joe Tester</name>\n"+
                "       <!-- this is a - multi line \n"+
                "            comment -->\n"+
                "       <street>Baker street 5</street>\n"+
                "   </address>\n"+
                "</addresses>";
        JSONObject jsonObject = XML.toJSONObject(xmlStr);
        String expectedStr = "{\"addresses\":{\"address\":{\"street\":\"Baker "+
                "street 5\",\"name\":\"Joe Tester\",\"content\":\" this is -- "+
                "<another> comment \"}}}";
        JSONObject expectedJsonObject = new JSONObject(expectedStr);
        Util.compareActualVsExpectedJsonObjects(jsonObject,expectedJsonObject);
    }

    /**
     * Valid XML to XML.toString()
     */
    @Test
    void shouldHandleToString() {
        String xmlStr = 
            "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n"+
            "<addresses xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\""+
            "   xsi:noNamespaceSchemaLocation='test.xsd'>\n"+
            "   <address>\n"+
            "       <name>[CDATA[Joe &amp; T &gt; e &lt; s &quot; t &apos; er]]</name>\n"+
            "       <street>Baker street 5</street>\n"+
            "       <ArrayOfNum>1, 2, 3, 4.1, 5.2</ArrayOfNum>\n"+
            "   </address>\n"+
            "</addresses>";

        String expectedStr = 
                "{\"addresses\":{\"address\":{\"street\":\"Baker street 5\","+
                "\"name\":\"[CDATA[Joe & T > e < s \\\" t \\\' er]]\","+
                "\"ArrayOfNum\":\"1, 2, 3, 4.1, 5.2\"\n"+
                "},\"xsi:noNamespaceSchemaLocation\":"+
                "\"test.xsd\",\"xmlns:xsi\":\"http://www.w3.org/2001/"+
                "XMLSchema-instance\"}}";
        
        JSONObject jsonObject = XML.toJSONObject(xmlStr);
        String xmlToStr = XML.toString(jsonObject);
        JSONObject finalJsonObject = XML.toJSONObject(xmlToStr);
        JSONObject expectedJsonObject = new JSONObject(expectedStr);
        Util.compareActualVsExpectedJsonObjects(jsonObject,expectedJsonObject);
        Util.compareActualVsExpectedJsonObjects(finalJsonObject,expectedJsonObject);
    }

    /**
     * Converting a JSON doc containing '>' content to JSONObject, then
     * XML.toString() should result in valid XML.
     */
    @Test
    void shouldHandleContentNoArraytoString() {
        String expectedStr = "{\"addresses\":{\"content\":\">\"}}";
        JSONObject expectedJsonObject = new JSONObject(expectedStr);
        String finalStr = XML.toString(expectedJsonObject);
        String expectedFinalStr = "<addresses>&gt;</addresses>";
        assertEquals(expectedFinalStr, finalStr, "Should handle expectedFinal: ["+expectedStr+"] final: ["+
                finalStr+"]");
    }

    /**
     * Converting a JSON doc containing a 'content' array to JSONObject, then
     * XML.toString() should result in valid XML.
     * TODO: This is probably an error in how the 'content' keyword is used.
     */
    @Test
    void shouldHandleContentArraytoString() {
        String expectedStr = 
            "{\"addresses\":{" +
            "\"content\":[1, 2, 3]}}";
        JSONObject expectedJsonObject = new JSONObject(expectedStr);
        String finalStr = XML.toString(expectedJsonObject);
        String expectedFinalStr = "<addresses>"+
                "1\n2\n3</addresses>";
        assertEquals(expectedFinalStr, finalStr, "Should handle expectedFinal: ["+expectedStr+"] final: ["+
                finalStr+"]");
    }

    /**
     * Converting a JSON doc containing a named array to JSONObject, then
     * XML.toString() should result in valid XML.
     */
    @Test
    void shouldHandleArraytoString() {
        String expectedStr = 
            "{\"addresses\":{"+
            "\"something\":[1, 2, 3]}}";
        JSONObject expectedJsonObject = new JSONObject(expectedStr);
        String finalStr = XML.toString(expectedJsonObject);
        String expectedFinalStr = "<addresses>"+
                "<something>1</something><something>2</something><something>3</something>"+
                "</addresses>";
        assertEquals(expectedFinalStr, finalStr, "Should handle expectedFinal: ["+expectedStr+"] final: ["+
                finalStr+"]");
    }

    /**
     * Tests that the XML output for empty arrays is consistent.
     */
    @Test
    void shouldHandleEmptyArray(){
        final JSONObject jo1 = new JSONObject();
        jo1.put("array",new Object[]{});
        final JSONObject jo2 = new JSONObject();
        jo2.put("array",new JSONArray());

        final String expected = "<jo></jo>";
        String output1 = XML.toString(jo1,"jo");
        assertEquals(expected, output1, "Expected an empty root tag");
        String output2 = XML.toString(jo2,"jo");
        assertEquals(expected, output2, "Expected an empty root tag");
    }

    /**
     * Tests that the XML output for arrays is consistent when an internal array is empty.
     */
    @Test
    void shouldHandleEmptyMultiArray(){
        final JSONObject jo1 = new JSONObject();
        jo1.put("arr",new Object[]{"One", new String[]{}, "Four"});
        final JSONObject jo2 = new JSONObject();
        jo2.put("arr",new JSONArray(new Object[]{"One", new JSONArray(new String[]{}), "Four"}));

        final String expected = "<jo><arr>One</arr><arr></arr><arr>Four</arr></jo>";
        String output1 = XML.toString(jo1,"jo");
        assertEquals(expected, output1, "Expected a matching array");
        String output2 = XML.toString(jo2,"jo");
        assertEquals(expected, output2, "Expected a matching array");
    }

    /**
     * Tests that the XML output for arrays is consistent when arrays are not empty.
     */
    @Test
    void shouldHandleNonEmptyArray(){
        final JSONObject jo1 = new JSONObject();
        jo1.put("arr",new String[]{"One", "Two", "Three"});
        final JSONObject jo2 = new JSONObject();
        jo2.put("arr",new JSONArray(new String[]{"One", "Two", "Three"}));

        final String expected = "<jo><arr>One</arr><arr>Two</arr><arr>Three</arr></jo>";
        String output1 = XML.toString(jo1,"jo");
        assertEquals(expected, output1, "Expected a non empty root tag");
        String output2 = XML.toString(jo2,"jo");
        assertEquals(expected, output2, "Expected a non empty root tag");
    }

    /**
     * Tests that the XML output for arrays is consistent when arrays are not empty and contain internal arrays.
     */
    @Test
    void shouldHandleMultiArray(){
        final JSONObject jo1 = new JSONObject();
        jo1.put("arr",new Object[]{"One", new String[]{"Two", "Three"}, "Four"});
        final JSONObject jo2 = new JSONObject();
        jo2.put("arr",new JSONArray(new Object[]{"One", new JSONArray(new String[]{"Two", "Three"}), "Four"}));

        final String expected = "<jo><arr>One</arr><arr><array>Two</array><array>Three</array></arr><arr>Four</arr></jo>";
        String output1 = XML.toString(jo1,"jo");
        assertEquals(expected, output1, "Expected a matching array");
        String output2 = XML.toString(jo2,"jo");
        assertEquals(expected, output2, "Expected a matching array");
    }

    /**
     * Converting a JSON doc containing a named array of nested arrays to
     * JSONObject, then XML.toString() should result in valid XML.
     */
    @Test
    void shouldHandleNestedArraytoString() {
        String xmlStr = 
            "{\"addresses\":{\"address\":{\"name\":\"\",\"nocontent\":\"\","+
            "\"outer\":[[1], [2], [3]]},\"xsi:noNamespaceSchemaLocation\":\"test.xsd\",\""+
            "xmlns:xsi\":\"http://www.w3.org/2001/XMLSchema-instance\"}}";
        JSONObject jsonObject = new JSONObject(xmlStr);
        String finalStr = XML.toString(jsonObject);
        JSONObject finalJsonObject = XML.toJSONObject(finalStr);
        String expectedStr = "<addresses><address><name/><nocontent/>"+
                "<outer><array>1</array></outer><outer><array>2</array>"+
                "</outer><outer><array>3</array></outer>"+
                "</address><xsi:noNamespaceSchemaLocation>test.xsd</xsi:noName"+
                "spaceSchemaLocation><xmlns:xsi>http://www.w3.org/2001/XMLSche"+
                "ma-instance</xmlns:xsi></addresses>";
        JSONObject expectedJsonObject = XML.toJSONObject(expectedStr);
        Util.compareActualVsExpectedJsonObjects(finalJsonObject,expectedJsonObject);
    }


    /**
     * Possible bug: 
     * Illegal node-names must be converted to legal XML-node-names.
     * The given example shows 2 nodes which are valid for JSON, but not for XML.
     * Therefore illegal arguments should be converted to e.g. an underscore (_).
     */
    @Test
    void shouldHandleIllegalJSONNodeNames()
    {
        JSONObject inputJSON = new JSONObject();
        inputJSON.append("123IllegalNode", "someValue1");
        inputJSON.append("Illegal@node", "someValue2");

        String result = XML.toString(inputJSON);

        /*
         * This is invalid XML. Names should not begin with digits or contain
         * certain values, including '@'. One possible solution is to replace
         * illegal chars with '_', in which case the expected output would be:
         * <___IllegalNode>someValue1</___IllegalNode><Illegal_node>someValue2</Illegal_node>
         */
        String expected = "<123IllegalNode>someValue1</123IllegalNode><Illegal@node>someValue2</Illegal@node>";

        assertEquals(expected.length(), result.length(), "length");
        assertTrue(result.contains("<123IllegalNode>someValue1</123IllegalNode>"),"123IllegalNode");
        assertTrue(result.contains("<Illegal@node>someValue2</Illegal@node>"),"Illegal@node");
    }

    /**
     * JSONObject with NULL value, to XML.toString()
     */
    @Test
    void shouldHandleNullNodeValue()
    {
        JSONObject inputJSON = new JSONObject();
        inputJSON.put("nullValue", JSONObject.NULL);
        // This is a possible preferred result
        // String expectedXML = "<nullValue/>";
        /**
         * This is the current behavior. JSONObject.NULL is emitted as 
         * the string, "null".
         */
        String actualXML = "<nullValue>null</nullValue>";
        String resultXML = XML.toString(inputJSON);
        assertEquals(actualXML, resultXML);
    }

    /**
     * Investigate exactly how the "content" keyword works
     */
    @Test
    void contentOperations() {
        /*
         * When a standalone <!CDATA[...]] structure is found while parsing XML into a
         * JSONObject, the contents are placed in a string value with key="content".
         */
        String xmlStr = "<tag1></tag1><![CDATA[if (a < b && a > 0) then return]]><tag2></tag2>";
        JSONObject jsonObject = XML.toJSONObject(xmlStr);
        assertEquals(3, jsonObject.length(), "1. 3 items");
        assertEquals("", jsonObject.get("tag1"), "1. empty tag1");
        assertEquals("", jsonObject.get("tag2"), "1. empty tag2");
        assertEquals("if (a < b && a > 0) then return", jsonObject.get("content"), "1. content found");

        // multiple consecutive standalone cdatas are accumulated into an array
        xmlStr = "<tag1></tag1><![CDATA[if (a < b && a > 0) then return]]><tag2></tag2><![CDATA[here is another cdata]]>";
        jsonObject = XML.toJSONObject(xmlStr);
        assertEquals(3, jsonObject.length(), "2. 3 items");
        assertEquals("", jsonObject.get("tag1"), "2. empty tag1");
        assertEquals("", jsonObject.get("tag2"), "2. empty tag2");
        assertTrue(jsonObject.get("content") instanceof JSONArray, "2. content array found");
        JSONArray jsonArray = jsonObject.getJSONArray("content");
        assertEquals(2, jsonArray.length(), "2. array size");
        assertEquals("if (a < b && a > 0) then return", jsonArray.get(0), "2. content array entry 0");
        assertEquals("here is another cdata", jsonArray.get(1), "2. content array entry 1");

        /*
         * text content is accumulated in a "content" inside a local JSONObject.
         * If there is only one instance, it is saved in the context (a different JSONObject 
         * from the calling code. and the content element is discarded. 
         */
        xmlStr =  "<tag1>value 1</tag1>";
        jsonObject = XML.toJSONObject(xmlStr);
        assertEquals(1, jsonObject.length(), "3. 2 items");
        assertEquals("value 1", jsonObject.get("tag1"), "3. value tag1");

        /*
         * array-style text content (multiple tags with the same name) is 
         * accumulated in a local JSONObject with key="content" and value=JSONArray,
         * saved in the context, and then the local JSONObject is discarded.
         */
        xmlStr =  "<tag1>value 1</tag1><tag1>2</tag1><tag1>true</tag1>";
        jsonObject = XML.toJSONObject(xmlStr);
        assertEquals(1, jsonObject.length(), "4. 1 item");
        assertTrue(jsonObject.get("tag1") instanceof JSONArray, "4. content array found");
        jsonArray = jsonObject.getJSONArray("tag1");
        assertEquals(3, jsonArray.length(), "4. array size");
        assertEquals("value 1", jsonArray.get(0), "4. content array entry 0");
        assertEquals(2, jsonArray.getInt(1), "4. content array entry 1");
        assertTrue(jsonArray.getBoolean(2), "4. content array entry 2");

        /*
         * Complex content is accumulated in a "content" field. For example, an element
         * may contain a mix of child elements and text. Each text segment is 
         * accumulated to content. 
         */
        xmlStr =  "<tag1>val1<tag2/>val2</tag1>";
        jsonObject = XML.toJSONObject(xmlStr);
        assertEquals(1, jsonObject.length(), "5. 1 item");
        assertTrue(jsonObject.get("tag1") instanceof JSONObject, "5. jsonObject found");
        jsonObject = jsonObject.getJSONObject("tag1");
        assertEquals(2, jsonObject.length(), "5. 2 contained items");
        assertEquals("", jsonObject.get("tag2"), "5. contained tag");
        assertTrue(jsonObject.get("content") instanceof JSONArray, "5. contained content jsonArray found");
        jsonArray = jsonObject.getJSONArray("content");
        assertEquals(2, jsonArray.length(), "5. array size");
        assertEquals("val1", jsonArray.get(0), "5. content array entry 0");
        assertEquals("val2", jsonArray.get(1), "5. content array entry 1");

        /*
         * If there is only 1 complex text content, then it is accumulated in a 
         * "content" field as a string.
         */
        xmlStr =  "<tag1>val1<tag2/></tag1>";
        jsonObject = XML.toJSONObject(xmlStr);
        assertEquals(1, jsonObject.length(), "6. 1 item");
        assertTrue(jsonObject.get("tag1") instanceof JSONObject, "6. jsonObject found");
        jsonObject = jsonObject.getJSONObject("tag1");
        assertEquals("val1", jsonObject.get("content"), "6. contained content found");
        assertEquals("", jsonObject.get("tag2"), "6. contained tag2");

        /*
         * In this corner case, the content sibling happens to have key=content
         * We end up with an array within an array, and no content element.
         * This is probably a bug. 
         */
        xmlStr =  "<tag1>val1<content/></tag1>";
        jsonObject = XML.toJSONObject(xmlStr);
        assertEquals(1, jsonObject.length(), "7. 1 item");
        assertTrue(jsonObject.get("tag1") instanceof JSONArray, "7. jsonArray found");
        jsonArray = jsonObject.getJSONArray("tag1");
        assertEquals(1, jsonArray.length(), "array size 1");
        assertTrue(jsonArray.get(0) instanceof JSONArray, "7. contained array found");
        jsonArray = jsonArray.getJSONArray(0);
        assertEquals(2, jsonArray.length(), "7. inner array size 2");
        assertEquals("val1", jsonArray.get(0), "7. inner array item 0");
        assertEquals("", jsonArray.get(1), "7. inner array item 1");

        /*
         * Confirm behavior of original issue
         */
        String jsonStr = 
                "{"+
                    "\"Profile\": {"+
                        "\"list\": {"+
                            "\"history\": {"+
                                "\"entries\": ["+
                                    "{"+
                                        "\"deviceId\": \"id\","+
                                        "\"content\": {"+
                                            "\"material\": ["+
                                                "{"+
                                                    "\"stuff\": false"+
                                                "}"+
                                            "]"+
                                        "}"+
                                    "}"+
                                "]"+
                            "}"+
                        "}"+
                    "}"+
                "}";
        jsonObject = new JSONObject(jsonStr);
        xmlStr = XML.toString(jsonObject);
        /*
         * This is the created XML. Looks like content was mistaken for
         * complex (child node + text) XML. 
         *  <Profile>
         *      <list>
         *          <history>
         *              <entries>
         *                  <deviceId>id</deviceId>
         *                  {&quot;material&quot;:[{&quot;stuff&quot;:false}]}
         *              </entries>
         *          </history>
         *      </list>
         *  </Profile>
         */
        assertTrue(true, "nothing to test here, see comment on created XML, above");
    }

    /**
     * Convenience method, given an input string and expected result,
     * convert to JSONObject and compare actual to expected result.
     * @param xmlStr the string to parse
     * @param expectedStr the expected JSON string
     */
    private void compareStringToJSONObject(String xmlStr, String expectedStr) {
        JSONObject jsonObject = XML.toJSONObject(xmlStr);
        JSONObject expectedJsonObject = new JSONObject(expectedStr);
        Util.compareActualVsExpectedJsonObjects(jsonObject,expectedJsonObject);
    }

    /**
     * Convenience method, given an input string and expected result,
     * convert to JSONObject via reader and compare actual to expected result.
     * @param xmlStr the string to parse
     * @param expectedStr the expected JSON string
     */
    private void compareReaderToJSONObject(String xmlStr, String expectedStr) {
        JSONObject expectedJsonObject = new JSONObject(expectedStr);
        Reader reader = new StringReader(xmlStr);
        JSONObject jsonObject = XML.toJSONObject(reader);
        Util.compareActualVsExpectedJsonObjects(jsonObject,expectedJsonObject);
    }

    /**
     * Convenience method, given an input string and expected result, convert to
     * JSONObject via file and compare actual to expected result.
     * 
     * @param xmlStr
     *            the string to parse
     * @param expectedStr
     *            the expected JSON string
     * @throws IOException
     */
    private void compareFileToJSONObject(String xmlStr, String expectedStr) {
        assertDoesNotThrow(() -> {
            JSONObject expectedJsonObject = new JSONObject(expectedStr);
            File tempFile = File.createTempFile("fileToJSONObject.xml", null, this.testFolder);
            FileWriter fileWriter = new FileWriter(tempFile);
            try {
                fileWriter.write(xmlStr);
            } finally {
                fileWriter.close();
            }

            Reader reader = new FileReader(tempFile);
            try {
                JSONObject jsonObject = XML.toJSONObject(reader);
                Util.compareActualVsExpectedJsonObjects(jsonObject, expectedJsonObject);
            } finally {
                reader.close();
            }
        }, "Error: ");
    }

    /**
     * JSON string lost leading zero and converted "True" to true.
     */
    @Test
    void toJSONArray_jsonOutput() {
        final String originalXml = "<root><id>01</id><id>1</id><id>00</id><id>0</id><item id=\"01\"/><title>True</title></root>";
        final JSONObject expectedJson = new JSONObject("{\"root\":{\"item\":{\"id\":1},\"id\":[1,1,0,0],\"title\":true}}");
        final JSONObject actualJsonOutput = XML.toJSONObject(originalXml, false);

        Util.compareActualVsExpectedJsonObjects(actualJsonOutput,expectedJson);
    }

    /**
     * JSON string cannot be reverted to original xml.
     */
    @Test
    void toJSONArray_reversibility() {
        final String originalXml = "<root><id>01</id><id>1</id><id>00</id><id>0</id><item id=\"01\"/><title>True</title></root>";
        final String revertedXml = XML.toString(XML.toJSONObject(originalXml, false));

        assertNotEquals(revertedXml, originalXml);
    }

    /**
     * test passes when using the new method toJsonArray.
     */
    @Test
    void toJsonXML() {
        final String originalXml = "<root><id>01</id><id>1</id><id>00</id><id>0</id><item id=\"01\"/><title>True</title></root>";
        final JSONObject expected = new JSONObject("{\"root\":{\"item\":{\"id\":\"01\"},\"id\":[\"01\",\"1\",\"00\",\"0\"],\"title\":\"True\"}}");

        final JSONObject actual = XML.toJSONObject(originalXml,true);
        
        Util.compareActualVsExpectedJsonObjects(actual, expected);
        
        final String reverseXml = XML.toString(actual);
        // this reversal isn't exactly the same. use JSONML for an exact reversal
        // the order of the elements may be differnet as well.
        final String expectedReverseXml = "<root><item><id>01</id></item><id>01</id><id>1</id><id>00</id><id>0</id><title>True</title></root>";

        assertEquals(expectedReverseXml.length(), reverseXml.length(), "length");
        assertTrue(reverseXml.contains("<id>01</id><id>1</id><id>00</id><id>0</id>"), "array contents");
        assertTrue(reverseXml.contains("<item><id>01</id></item>"), "item contents");
        assertTrue(reverseXml.contains("<title>True</title>"), "title contents");
    }

    /**
     * test to validate certain conditions of XML unescaping.
     */
    @Test
    void unescape() {
        assertEquals("{\"xml\":\"Can cope <;\"}",
                XML.toJSONObject("<xml>Can cope &lt;; </xml>").toString());
        assertEquals("Can cope <; ", XML.unescape("Can cope &lt;; "));

        assertEquals("{\"xml\":\"Can cope & ;\"}",
                XML.toJSONObject("<xml>Can cope &amp; ; </xml>").toString());
        assertEquals("Can cope & ; ", XML.unescape("Can cope &amp; ; "));

        assertEquals("{\"xml\":\"Can cope &;\"}",
                XML.toJSONObject("<xml>Can cope &amp;; </xml>").toString());
        assertEquals("Can cope &; ", XML.unescape("Can cope &amp;; "));

        // unicode entity
        assertEquals("{\"xml\":\"Can cope 4;\"}",
                XML.toJSONObject("<xml>Can cope &#x34;; </xml>").toString());
        assertEquals("Can cope 4; ", XML.unescape("Can cope &#x34;; "));

        // double escaped
        assertEquals("{\"xml\":\"Can cope &lt;\"}",
                XML.toJSONObject("<xml>Can cope &amp;lt; </xml>").toString());
        assertEquals("Can cope &lt; ", XML.unescape("Can cope &amp;lt; "));
        
        assertEquals("{\"xml\":\"Can cope &#x34;\"}",
                XML.toJSONObject("<xml>Can cope &amp;#x34; </xml>").toString());
        assertEquals("Can cope &#x34; ", XML.unescape("Can cope &amp;#x34; "));

   }

    /**
     * test passes when xsi:nil="true" converting to null (JSON specification-like nil conversion enabled)
     */
    @Test
    void toJsonWithNullWhenNilConversionEnabled() {
        final String originalXml = "<root><id xsi:nil=\"true\"/></root>";
        final String expectedJsonString = "{\"root\":{\"id\":null}}";

        final JSONObject json = XML.toJSONObject(originalXml,
                new XMLParserConfiguration()
                    .withKeepStrings(false)
                    .withcDataTagName("content")
                    .withConvertNilAttributeToNull(true));
        assertEquals(expectedJsonString, json.toString());
    }

    /**
     * test passes when xsi:nil="true" not converting to null (JSON specification-like nil conversion disabled)
     */
    @Test
    void toJsonWithNullWhenNilConversionDisabled() {
        final String originalXml = "<root><id xsi:nil=\"true\"/></root>";
        final String expectedJsonString = "{\"root\":{\"id\":{\"xsi:nil\":true}}}";

        final JSONObject json = XML.toJSONObject(originalXml, new XMLParserConfiguration());
        assertEquals(expectedJsonString, json.toString());
    }

    /**
     * Tests to verify that supported escapes in XML are converted to actual values.
     */
    @Test
    void issue537CaseSensitiveHexEscapeMinimal(){
        String xmlStr = 
            "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n"+
            "<root>Neutrophils.Hypersegmented &#X7C; Bld-Ser-Plas</root>";
        String expectedStr = 
            "{\"root\":\"Neutrophils.Hypersegmented | Bld-Ser-Plas\"}";
        JSONObject xmlJSONObj = XML.toJSONObject(xmlStr, true);
        JSONObject expected = new JSONObject(expectedStr);
        Util.compareActualVsExpectedJsonObjects(xmlJSONObj, expected);
    }

    /**
     * Tests to verify that supported escapes in XML are converted to actual values.
     */
    @Test
    void issue537CaseSensitiveHexEscapeFullFile(){
        assertDoesNotThrow(() -> {
            InputStream xmlStream = null;
            try {
                xmlStream = XMLTest.class.getClassLoader().getResourceAsStream("Issue537.xml");
                Reader xmlReader = new InputStreamReader(xmlStream, Charset.forName("UTF-8"));
                JSONObject actual = XML.toJSONObject(xmlReader, true);
                InputStream jsonStream = null;
                try {
                    jsonStream = XMLTest.class.getClassLoader().getResourceAsStream("Issue537.json");
                    final JSONObject expected = new JSONObject(new JSONTokener(jsonStream));
                    Util.compareActualVsExpectedJsonObjects(actual, expected);
                } finally {
                    if (jsonStream != null) {
                        jsonStream.close();
                    }
                }
            } finally {
                if (xmlStream != null) {
                    xmlStream.close();
                }
            }
        }, "file writer error: ");
    }

    /**
     * Tests to verify that supported escapes in XML are converted to actual values.
     */
    @Test
    void issue537CaseSensitiveHexUnEscapeDirect(){
        String origStr = 
            "Neutrophils.Hypersegmented &#X7C; Bld-Ser-Plas";
        String expectedStr = 
            "Neutrophils.Hypersegmented | Bld-Ser-Plas";
        String actualStr = XML.unescape(origStr);
        
        assertEquals(expectedStr, actualStr, "Case insensitive Entity unescape");
    }

    /**
     * test passes when xsi:type="java.lang.String" not converting to string
     */
    @Test
    void toJsonWithTypeWhenTypeConversionDisabled() {
        String originalXml = "<root><id xsi:type=\"string\">1234</id></root>";
        String expectedJsonString = "{\"root\":{\"id\":{\"xsi:type\":\"string\",\"content\":1234}}}";
        JSONObject expectedJson = new JSONObject(expectedJsonString);
        JSONObject actualJson = XML.toJSONObject(originalXml, new XMLParserConfiguration());
        Util.compareActualVsExpectedJsonObjects(actualJson,expectedJson);
    }

    /**
     * test passes when xsi:type="java.lang.String" converting to String
     */
    @Test
    void toJsonWithTypeWhenTypeConversionEnabled() {
        String originalXml = "<root><id1 xsi:type=\"string\">1234</id1>"
                + "<id2 xsi:type=\"integer\">1234</id2></root>";
        String expectedJsonString = "{\"root\":{\"id2\":1234,\"id1\":\"1234\"}}";
        JSONObject expectedJson = new JSONObject(expectedJsonString);
        Map<String, XMLXsiTypeConverter<?>> xsiTypeMap = new HashMap<String, XMLXsiTypeConverter<?>>();
        xsiTypeMap.put("string", new XMLXsiTypeConverter<String>() {
            @Override public String convert(final String value) {
                return value;
            }
        });
        xsiTypeMap.put("integer", new XMLXsiTypeConverter<Integer>() {
            @Override public Integer convert(final String value) {
                return Integer.valueOf(value);
            }
        });
        JSONObject actualJson = XML.toJSONObject(originalXml, new XMLParserConfiguration().withXsiTypeMap(xsiTypeMap));
        Util.compareActualVsExpectedJsonObjects(actualJson,expectedJson);
    }

    @Test
    void toJsonWithXSITypeWhenTypeConversionEnabled() {
        String originalXml = "<root><asString xsi:type=\"string\">12345</asString><asInt "
                + "xsi:type=\"integer\">54321</asInt></root>";
        String expectedJsonString = "{\"root\":{\"asString\":\"12345\",\"asInt\":54321}}";
        JSONObject expectedJson = new JSONObject(expectedJsonString);
        Map<String, XMLXsiTypeConverter<?>> xsiTypeMap = new HashMap<String, XMLXsiTypeConverter<?>>();
        xsiTypeMap.put("string", new XMLXsiTypeConverter<String>() {
            @Override public String convert(final String value) {
                return value;
            }
        });
        xsiTypeMap.put("integer", new XMLXsiTypeConverter<Integer>() {
            @Override public Integer convert(final String value) {
                return Integer.valueOf(value);
            }
        });
        JSONObject actualJson = XML.toJSONObject(originalXml, new XMLParserConfiguration().withXsiTypeMap(xsiTypeMap));
        Util.compareActualVsExpectedJsonObjects(actualJson,expectedJson);
    }

    @Test
    void toJsonWithXSITypeWhenTypeConversionNotEnabledOnOne() {
        String originalXml = "<root><asString xsi:type=\"string\">12345</asString><asInt>54321</asInt></root>";
        String expectedJsonString = "{\"root\":{\"asString\":\"12345\",\"asInt\":54321}}";
        JSONObject expectedJson = new JSONObject(expectedJsonString);
        Map<String, XMLXsiTypeConverter<?>> xsiTypeMap = new HashMap<String, XMLXsiTypeConverter<?>>();
        xsiTypeMap.put("string", new XMLXsiTypeConverter<String>() {
            @Override public String convert(final String value) {
                return value;
            }
        });
        JSONObject actualJson = XML.toJSONObject(originalXml, new XMLParserConfiguration().withXsiTypeMap(xsiTypeMap));
        Util.compareActualVsExpectedJsonObjects(actualJson,expectedJson);
    }

    @Test
    void xSITypeMapNotModifiable() {
        Map<String, XMLXsiTypeConverter<?>> xsiTypeMap = new HashMap<String, XMLXsiTypeConverter<?>>();
        XMLParserConfiguration config = new XMLParserConfiguration().withXsiTypeMap(xsiTypeMap);
        xsiTypeMap.put("string", new XMLXsiTypeConverter<String>() {
            @Override public String convert(final String value) {
                return value;
            }
        });
        assertEquals(0, config.getXsiTypeMap().size(), "Config Conversion Map size is expected to be 0");

        try {
            config.getXsiTypeMap().put("boolean", new XMLXsiTypeConverter<Boolean>() {
                @Override public Boolean convert(final String value) {
                    return Boolean.valueOf(value);
                }
            });
            fail("Expected to be unable to modify the config");
        } catch (Exception ignored) { }
    }

    @Test
    void indentComplicatedJsonObject(){
        String str = "{\n" +
                "  \"success\": true,\n" +
                "  \"error\": null,\n" +
                "  \"response\": [\n" +
                "    {\n" +
                "      \"timestamp\": 1664917200,\n" +
                "      \"dateTimeISO\": \"2022-10-05T00:00:00+03:00\",\n" +
                "      \"loc\": {\n" +
                "        \"lat\": 39.91987,\n" +
                "        \"long\": 32.85427\n" +
                "      },\n" +
                "      \"place\": {\n" +
                "        \"name\": \"ankara\",\n" +
                "        \"state\": \"an\",\n" +
                "        \"country\": \"tr\"\n" +
                "      },\n" +
                "      \"profile\": {\n" +
                "        \"tz\": \"Europe/Istanbul\"\n" +
                "      },\n" +
                "      \"sun\": {\n" +
                "        \"rise\": 1664941721,\n" +
                "        \"riseISO\": \"2022-10-05T06:48:41+03:00\",\n" +
                "        \"set\": 1664983521,\n" +
                "        \"setISO\": \"2022-10-05T18:25:21+03:00\",\n" +
                "        \"transit\": 1664962621,\n" +
                "        \"transitISO\": \"2022-10-05T12:37:01+03:00\",\n" +
                "        \"midnightSun\": false,\n" +
                "        \"polarNight\": false,\n" +
                "        \"twilight\": {\n" +
                "          \"civilBegin\": 1664940106,\n" +
                "          \"civilBeginISO\": \"2022-10-05T06:21:46+03:00\",\n" +
                "          \"civilEnd\": 1664985136,\n" +
                "          \"civilEndISO\": \"2022-10-05T18:52:16+03:00\",\n" +
                "          \"nauticalBegin\": 1664938227,\n" +
                "          \"nauticalBeginISO\": \"2022-10-05T05:50:27+03:00\",\n" +
                "          \"nauticalEnd\": 1664987015,\n" +
                "          \"nauticalEndISO\": \"2022-10-05T19:23:35+03:00\",\n" +
                "          \"astronomicalBegin\": 1664936337,\n" +
                "          \"astronomicalBeginISO\": \"2022-10-05T05:18:57+03:00\",\n" +
                "          \"astronomicalEnd\": 1664988905,\n" +
                "          \"astronomicalEndISO\": \"2022-10-05T19:55:05+03:00\"\n" +
                "        }\n" +
                "      },\n" +
                "      \"moon\": {\n" +
                "        \"rise\": 1664976480,\n" +
                "        \"riseISO\": \"2022-10-05T16:28:00+03:00\",\n" +
                "        \"set\": 1664921520,\n" +
                "        \"setISO\": \"2022-10-05T01:12:00+03:00\",\n" +
                "        \"transit\": 1664994240,\n" +
                "        \"transitISO\": \"2022-10-05T21:24:00+03:00\",\n" +
                "        \"underfoot\": 1664949360,\n" +
                "        \"underfootISO\": \"2022-10-05T08:56:00+03:00\",\n" +
                "        \"phase\": {\n" +
                "          \"phase\": 0.3186,\n" +
                "          \"name\": \"waxing gibbous\",\n" +
                "          \"illum\": 71,\n" +
                "          \"age\": 9.41,\n" +
                "          \"angle\": 0.55\n" +
                "        }\n" +
                "      }\n" +
                "    }\n" +
                "  ]\n" +
                "}" ;
        JSONObject jsonObject = new JSONObject(str);
        String actualIndentedXmlString = XML.toString(jsonObject, 1);
        JSONObject actualJsonObject = XML.toJSONObject(actualIndentedXmlString);
        String expected = "<success>true</success>\n" +
                "<response>\n" +
                " <dateTimeISO>2022-10-05T00:00:00+03:00</dateTimeISO>\n" +
                " <loc>\n" +
                "  <lat>39.91987</lat>\n" +
                "  <long>32.85427</long>\n" +
                " </loc>\n" +
                " <moon>\n" +
                "  <phase>\n" +
                "   <phase>0.3186</phase>\n" +
                "   <name>waxing gibbous</name>\n" +
                "   <angle>0.55</angle>\n" +
                "   <illum>71</illum>\n" +
                "   <age>9.41</age>\n" +
                "  </phase>\n" +
                "  <setISO>2022-10-05T01:12:00+03:00</setISO>\n" +
                "  <underfoot>1664949360</underfoot>\n" +
                "  <set>1664921520</set>\n" +
                "  <transit>1664994240</transit>\n" +
                "  <transitISO>2022-10-05T21:24:00+03:00</transitISO>\n" +
                "  <riseISO>2022-10-05T16:28:00+03:00</riseISO>\n" +
                "  <rise>1664976480</rise>\n" +
                "  <underfootISO>2022-10-05T08:56:00+03:00</underfootISO>\n" +
                " </moon>\n" +
                " <profile>\n" +
                "  <tz>Europe/Istanbul</tz>\n" +
                " </profile>\n" +
                " <place>\n" +
                "  <country>tr</country>\n" +
                "  <name>ankara</name>\n" +
                "  <state>an</state>\n" +
                " </place>\n" +
                " <sun>\n" +
                "  <setISO>2022-10-05T18:25:21+03:00</setISO>\n" +
                "  <midnightSun>false</midnightSun>\n" +
                "  <set>1664983521</set>\n" +
                "  <transit>1664962621</transit>\n" +
                "  <polarNight>false</polarNight>\n" +
                "  <transitISO>2022-10-05T12:37:01+03:00</transitISO>\n" +
                "  <riseISO>2022-10-05T06:48:41+03:00</riseISO>\n" +
                "  <rise>1664941721</rise>\n" +
                "  <twilight>\n" +
                "   <civilEnd>1664985136</civilEnd>\n" +
                "   <astronomicalBegin>1664936337</astronomicalBegin>\n" +
                "   <astronomicalEnd>1664988905</astronomicalEnd>\n" +
                "   <astronomicalBeginISO>2022-10-05T05:18:57+03:00</astronomicalBeginISO>\n" +
                "   <civilBegin>1664940106</civilBegin>\n" +
                "   <nauticalEndISO>2022-10-05T19:23:35+03:00</nauticalEndISO>\n" +
                "   <astronomicalEndISO>2022-10-05T19:55:05+03:00</astronomicalEndISO>\n" +
                "   <nauticalBegin>1664938227</nauticalBegin>\n" +
                "   <nauticalEnd>1664987015</nauticalEnd>\n" +
                "   <nauticalBeginISO>2022-10-05T05:50:27+03:00</nauticalBeginISO>\n" +
                "   <civilBeginISO>2022-10-05T06:21:46+03:00</civilBeginISO>\n" +
                "   <civilEndISO>2022-10-05T18:52:16+03:00</civilEndISO>\n" +
                "  </twilight>\n" +
                " </sun>\n" +
                " <timestamp>1664917200</timestamp>\n" +
                "</response>\n" +
                "<error>null</error>\n";
        JSONObject expectedJsonObject = XML.toJSONObject(expected);
        assertTrue(expectedJsonObject.similar(actualJsonObject));


    }

    @Test
    void shouldCreateExplicitEndTagWithEmptyValueWhenConfigured(){
        String jsonString = "{outer:{innerOne:\"\", innerTwo:\"two\"}}";
        JSONObject jsonObject = new JSONObject(jsonString);
        String expectedXmlString = "<encloser><outer><innerOne></innerOne><innerTwo>two</innerTwo></outer></encloser>";
        String xmlForm = XML.toString(jsonObject,"encloser", new XMLParserConfiguration().withCloseEmptyTag(true));
        JSONObject actualJsonObject = XML.toJSONObject(xmlForm);
        JSONObject expectedJsonObject = XML.toJSONObject(expectedXmlString);
        assertTrue(expectedJsonObject.similar(actualJsonObject));
    }

    @Test
    void shouldNotCreateExplicitEndTagWithEmptyValueWhenNotConfigured(){
        String jsonString = "{outer:{innerOne:\"\", innerTwo:\"two\"}}";
        JSONObject jsonObject = new JSONObject(jsonString);
        String expectedXmlString = "<encloser><outer><innerOne/><innerTwo>two</innerTwo></outer></encloser>";
        String xmlForm = XML.toString(jsonObject,"encloser", new XMLParserConfiguration().withCloseEmptyTag(false));
        JSONObject actualJsonObject = XML.toJSONObject(xmlForm);
        JSONObject expectedJsonObject = XML.toJSONObject(expectedXmlString);
        assertTrue(expectedJsonObject.similar(actualJsonObject));
    }


    @Test
    void indentSimpleJsonObject(){
        String str = "{    \"employee\": {  \n" +
                "        \"name\":       \"sonoo\",   \n" +
                "        \"salary\":      56000,   \n" +
                "        \"married\":    true  \n" +
                "    }}";
        JSONObject jsonObject = new JSONObject(str);
        String actual = XML.toString(jsonObject, "Test", 2);
        JSONObject actualJsonObject = XML.toJSONObject(actual);
        String expected = "<Test>\n" +
                "  <employee>\n" +
                "    <name>sonoo</name>\n" +
                "    <salary>56000</salary>\n" +
                "    <married>true</married>\n" +
                "  </employee>\n" +
                "</Test>\n";
        JSONObject expectedJsonObject = XML.toJSONObject(expected);
        assertTrue(expectedJsonObject.similar(actualJsonObject));
    }

    @Test
    void indentSimpleJsonArray(){
        String str = "[  \n" +
                "    {\"name\":\"Ram\", \"email\":\"Ram@gmail.com\"},  \n" +
                "    {\"name\":\"Bob\", \"email\":\"bob32@gmail.com\"}  \n" +
                "]  ";
        JSONArray jsonObject = new JSONArray(str);
        String actual = XML.toString(jsonObject, 2);
        JSONObject actualJsonObject = XML.toJSONObject(actual);
        String expected = "<array>\n" +
                "  <name>Ram</name>\n" +
                "  <email>Ram@gmail.com</email>\n" +
                "</array>\n" +
                "<array>\n" +
                "  <name>Bob</name>\n" +
                "  <email>bob32@gmail.com</email>\n" +
                "</array>\n";
        JSONObject expectedJsonObject = XML.toJSONObject(expected);
        assertTrue(expectedJsonObject.similar(actualJsonObject));


    }

    @Test
    void indentComplicatedJsonObjectWithArrayAndWithConfig(){
        try (InputStream jsonStream = XMLTest.class.getClassLoader().getResourceAsStream("Issue593.json")) {
            final JSONObject object = new JSONObject(new JSONTokener(jsonStream));
            String actualString = XML.toString(object, null, XMLParserConfiguration.KEEP_STRINGS, 2);
            try (InputStream xmlStream = XMLTest.class.getClassLoader().getResourceAsStream("Issue593.xml")) {
                int bufferSize = 1024;
                char[] buffer = new char[bufferSize];
                StringBuilder expected = new StringBuilder();
                Reader in = new InputStreamReader(xmlStream, "UTF-8");
                for (int numRead; (numRead = in.read(buffer, 0, buffer.length)) > 0; ) {
                    expected.append(buffer, 0, numRead);
                }
                assertTrue(XML.toJSONObject(expected.toString()).similar(XML.toJSONObject(actualString)));
            }
        } catch (IOException e) {
            fail("file writer error: " +e.getMessage());
        }
    }

    @Test
    void maxNestingDepthOf42IsRespected() {
        final String wayTooLongMalformedXML = new String(new char[6000]).replace("\0", "<a>");

        final int maxNestingDepth = 42;

        try {
            XML.toJSONObject(wayTooLongMalformedXML, XMLParserConfiguration.ORIGINAL.withMaxNestingDepth(maxNestingDepth));

            fail("Expecting a JSONException");
        } catch (JSONException e) {
            assertTrue(e.getMessage().startsWith("Maximum nesting depth of " + maxNestingDepth),
                "Wrong throwable thrown: not expecting message <" + e.getMessage() + ">");
        }
    }

    @Test
    void maxNestingDepthIsRespectedWithValidXML() {
        final String perfectlyFineXML = "<Test>\n" +
            "  <employee>\n" +
            "    <name>sonoo</name>\n" +
            "    <salary>56000</salary>\n" +
            "    <married>true</married>\n" +
            "  </employee>\n" +
            "</Test>\n";

        final int maxNestingDepth = 1;

        try {
            XML.toJSONObject(perfectlyFineXML, XMLParserConfiguration.ORIGINAL.withMaxNestingDepth(maxNestingDepth));

            fail("Expecting a JSONException");
        } catch (JSONException e) {
            assertTrue(e.getMessage().startsWith("Maximum nesting depth of " + maxNestingDepth),
                "Wrong throwable thrown: not expecting message <" + e.getMessage() + ">");
        }
    }

    @Test
    void maxNestingDepthWithValidFittingXML() {
        final String perfectlyFineXML = "<Test>\n" +
            "  <employee>\n" +
            "    <name>sonoo</name>\n" +
            "    <salary>56000</salary>\n" +
            "    <married>true</married>\n" +
            "  </employee>\n" +
            "</Test>\n";

        final int maxNestingDepth = 3;

        try {
            XML.toJSONObject(perfectlyFineXML, XMLParserConfiguration.ORIGINAL.withMaxNestingDepth(maxNestingDepth));
        } catch (JSONException e) {
            e.printStackTrace();
            fail("XML document should be parsed as its maximum depth fits the maxNestingDepth " +
                "parameter of the XMLParserConfiguration used");
        }
    }
}



