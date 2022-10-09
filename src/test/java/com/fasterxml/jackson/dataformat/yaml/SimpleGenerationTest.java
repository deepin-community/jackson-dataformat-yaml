package com.fasterxml.jackson.dataformat.yaml;

import java.io.*;
import java.util.*;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

public class SimpleGenerationTest extends ModuleTestBase
{
    public void testStreamingArray() throws Exception
    {
        YAMLFactory f = new YAMLFactory();
        StringWriter w = new StringWriter();
        JsonGenerator gen = f.createGenerator(w);
        gen.writeStartArray();
        gen.writeNumber(3);
        gen.writeString("foobar");
        gen.writeEndArray();
        gen.close();

        String yaml = w.toString();
        // should probably parse?
        // note: 1.12 uses more compact notation; 1.10 has prefix
        yaml = trimDocMarker(yaml).trim();
        assertEquals("- 3\n- \"foobar\"", yaml);
    }

    public void testStreamingObject() throws Exception
    {
        YAMLFactory f = new YAMLFactory();
        StringWriter w = new StringWriter();
        JsonGenerator gen = f.createGenerator(w);
        _writeBradDoc(gen);
        String yaml = w.toString();

        // note: 1.12 uses more compact notation; 1.10 has prefix
        yaml = trimDocMarker(yaml).trim();
        assertEquals("name: \"Brad\"\nage: 39", yaml);
        gen.close();
    }

    public void testStreamingNested() throws Exception
    {
        YAMLFactory f = new YAMLFactory();
        StringWriter w = new StringWriter();
        JsonGenerator gen = f.createGenerator(w);

        gen.writeStartObject();
        gen.writeFieldName("ob");
        gen.writeStartArray();
        gen.writeString("a");
        gen.writeString("b");
        gen.writeEndArray();
        gen.writeEndObject();

        gen.close();

        String yaml = w.toString();

        // note: 1.12 uses more compact notation; 1.10 has prefix
        yaml = trimDocMarker(yaml).trim();

        BufferedReader br = new BufferedReader(new StringReader(yaml));
        assertEquals("ob:", br.readLine());

        /* 27-Jan-2015, tatu: Not 100% if those items ought to (or not) be indented.
         *   SnakeYAML doesn't do that; yet some libs expect it. Strange.
         */
        assertEquals("- \"a\"", br.readLine());
        assertEquals("- \"b\"", br.readLine());
        assertNull(br.readLine());
        br.close();
    }

    public void testBasicPOJO() throws Exception
    {
        ObjectMapper mapper = mapperForYAML();
        FiveMinuteUser user = new FiveMinuteUser("Bob", "Dabolito", false,
                FiveMinuteUser.Gender.MALE, new byte[] { 1, 3, 13, 79 });
        String yaml = mapper.writeValueAsString(user).trim();
        String[] parts = yaml.split("\n");
        boolean gotHeader = (parts.length == 6);
        if (!gotHeader) {
            // 1.10 has 6 as it has header
            assertEquals(5, parts.length);
        }
        // unify ordering, need to use TreeSets
        TreeSet<String> exp = new TreeSet<String>();
        for (String part : parts) {
            exp.add(part.trim());
        }
        Iterator<String> it = exp.iterator();
        if (gotHeader) {
            assertEquals("---", it.next());
        }
        assertEquals("firstName: \"Bob\"", it.next());
        assertEquals("gender: \"MALE\"", it.next());
        assertEquals("lastName: \"Dabolito\"", it.next());
        assertEquals("userImage: \"AQMNTw==\"", it.next());
        assertEquals("verified: false", it.next());
    }

    // Issue#12:
    public void testWithFile() throws Exception
    {
        File f = File.createTempFile("test", ".yml");
        f.deleteOnExit();
        ObjectMapper mapper = mapperForYAML();
        Map<String,Integer> map = new HashMap<String,Integer>();
        map.put("a", 3);
        mapper.writeValue(f, map);
        assertTrue(f.canRead());
        BufferedReader br = new BufferedReader(new InputStreamReader(new FileInputStream(
                f), "UTF-8"));
        String doc = br.readLine();
        String str = br.readLine();
        if (str != null) {
            doc += "\n" + str;
        }
        doc = trimDocMarker(doc);
        assertEquals("a: 3", doc);
        br.close();
        f.delete();
    }

    public void testWithFile2() throws Exception
    {
        File f = File.createTempFile("test", ".yml");
        f.deleteOnExit();
        ObjectMapper mapper = mapperForYAML();
        ObjectNode root = mapper.createObjectNode();
        root.put("name", "Foobar");
        mapper.writeValue(f, root);

        // and get it back
        Map<?,?> result = mapper.readValue(f, Map.class);
        assertEquals(1, result.size());
        assertEquals("Foobar", result.get("name"));
    }

    @SuppressWarnings("resource")
    public void testStartMarker() throws Exception
    {
        YAMLFactory f = new YAMLFactory();

        // Ok, first, assume we do get the marker:
        StringWriter w = new StringWriter();
        assertTrue(f.isEnabled(YAMLGenerator.Feature.WRITE_DOC_START_MARKER));
        YAMLGenerator gen = f.createGenerator(w);
        assertTrue(gen.isEnabled(YAMLGenerator.Feature.WRITE_DOC_START_MARKER));
        _writeBradDoc(gen);
        String yaml = w.toString().trim();
        assertEquals("---\nname: \"Brad\"\nage: 39", yaml);

        // and then, disabling, and not any more
        f.disable(YAMLGenerator.Feature.WRITE_DOC_START_MARKER);
        assertFalse(f.isEnabled(YAMLGenerator.Feature.WRITE_DOC_START_MARKER));
        w = new StringWriter();
        gen = f.createGenerator(w);
        assertFalse(gen.isEnabled(YAMLGenerator.Feature.WRITE_DOC_START_MARKER));
        _writeBradDoc(gen);
        yaml = w.toString().trim();
        assertEquals("name: \"Brad\"\nage: 39", yaml);
    }

    public void testSplitLines() throws Exception
    {
        final String TEXT = "1234567890 1234567890 1234567890 1234567890 1234567890 1234567890 1234567890 1234567890 1234567890";
        final String[] INPUT = new String[] { TEXT };
        YAMLFactory f = new YAMLFactory();

        // verify default settings
        assertTrue(f.isEnabled(YAMLGenerator.Feature.SPLIT_LINES));

        // and first write with splitting enabled
        YAMLMapper mapper = new YAMLMapper(f);
        String yaml = mapper.writeValueAsString(INPUT).trim();

        assertEquals("---\n" +
                "- \"1234567890 1234567890 1234567890 1234567890 1234567890 1234567890 1234567890 1234567890\\\n" +
                "  \\ 1234567890\"",
                yaml);

        // and then with splitting disabled
        f.disable(YAMLGenerator.Feature.SPLIT_LINES);

        yaml = mapper.writeValueAsString(INPUT).trim();
        assertEquals("---\n" +
                "- \"1234567890 1234567890 1234567890 1234567890 1234567890 1234567890 1234567890 1234567890 1234567890\"",
                yaml);
    }

    public void testLiteralStringsSingleLine() throws Exception
    {
        YAMLFactory f = new YAMLFactory();
        // verify default settings
        assertFalse(f.isEnabled(YAMLGenerator.Feature.MINIMIZE_QUOTES));

        f.configure(YAMLGenerator.Feature.MINIMIZE_QUOTES, true);

        YAMLMapper mapper = new YAMLMapper(f);

        Map<String, Object> content = new HashMap<String, Object>();
        content.put("key", "some value");
        String yaml = mapper.writeValueAsString(content).trim();

        assertEquals("---\n" +
                "key: some value", yaml);
    }

    public void testMinimizeQuotesWithBooleanContent() throws Exception
    {
        YAMLFactory f = new YAMLFactory();
        f.configure(YAMLGenerator.Feature.MINIMIZE_QUOTES, true);

        YAMLMapper mapper = new YAMLMapper(f);

        Map<String, Object> content = new HashMap<String, Object>();
        content.put("key", "true");
        String yaml = mapper.writeValueAsString(content).trim();

        assertEquals("---\n" +
                "key: \"true\"", yaml);

        content.clear();
        content.put("key", "false");
        yaml = mapper.writeValueAsString(content).trim();

        assertEquals("---\n" +
                "key: \"false\"", yaml);

        content.clear();
        content.put("key", "something else");
        yaml = mapper.writeValueAsString(content).trim();

        assertEquals("---\n" +
                "key: something else", yaml);

        content.clear();
        content.put("key", Boolean.TRUE);
        yaml = mapper.writeValueAsString(content).trim();

        assertEquals("---\n" +
                "key: true", yaml);

    }

    public void testLiteralStringsMultiLine() throws Exception
    {
        YAMLFactory f = new YAMLFactory();
        // verify default settings
        assertFalse(f.isEnabled(YAMLGenerator.Feature.MINIMIZE_QUOTES));

        f.configure(YAMLGenerator.Feature.MINIMIZE_QUOTES, true);

        YAMLMapper mapper = new YAMLMapper(f);

        Map<String, Object> content = new HashMap<String, Object>();
        content.put("key", "first\nsecond\nthird");
        String yaml = mapper.writeValueAsString(content).trim();

        assertEquals("---\n" +
                "key: |-\n  first\n  second\n  third", yaml);
    }

    public void testQuoteNumberStoredAsString() throws Exception
    {
        YAMLFactory f = new YAMLFactory();
        // verify default settings
        assertFalse(f.isEnabled(YAMLGenerator.Feature.MINIMIZE_QUOTES));
        assertFalse(f.isEnabled(YAMLGenerator.Feature.ALWAYS_QUOTE_NUMBERS_AS_STRINGS));

        f.configure(YAMLGenerator.Feature.MINIMIZE_QUOTES, true);
        f.configure(YAMLGenerator.Feature.ALWAYS_QUOTE_NUMBERS_AS_STRINGS, true);

        YAMLMapper mapper = new YAMLMapper(f);

        Map<String, Object> content = new HashMap<String, Object>();
        content.put("key", "20");
        String yaml = mapper.writeValueAsString(content).trim();

        assertEquals("---\n" +
                "key: \"20\"", yaml);

        content.clear();
        content.put("key", "2.0");
        yaml = mapper.writeValueAsString(content).trim();

        assertEquals("---\n" +
                "key: \"2.0\"", yaml);

        content.clear();
        content.put("key", "2.0.1.2.3");
        yaml = mapper.writeValueAsString(content).trim();

        assertEquals("---\n" +
                "key: 2.0.1.2.3", yaml);
    }

    public void testNonQuoteNumberStoredAsString() throws Exception
    {
        YAMLFactory f = new YAMLFactory();
        // verify default settings
        assertFalse(f.isEnabled(YAMLGenerator.Feature.MINIMIZE_QUOTES));
        assertFalse(f.isEnabled(YAMLGenerator.Feature.ALWAYS_QUOTE_NUMBERS_AS_STRINGS));

        f.configure(YAMLGenerator.Feature.MINIMIZE_QUOTES, true);

        YAMLMapper mapper = new YAMLMapper(f);

        Map<String, Object> content = new HashMap<String, Object>();
        content.put("key", "20");
        String yaml = mapper.writeValueAsString(content).trim();

        assertEquals("---\n" +
                "key: 20", yaml);

        content.clear();
        content.put("key", "2.0");
        yaml = mapper.writeValueAsString(content).trim();

        assertEquals("---\n" +
                "key: 2.0", yaml);

        content.clear();
        content.put("key", "2.0.1.2.3");
        yaml = mapper.writeValueAsString(content).trim();

        assertEquals("---\n" +
                "key: 2.0.1.2.3", yaml);
    }

    /*
    /**********************************************************************
    /* Helper methods
    /**********************************************************************
     */


    protected void _writeBradDoc(JsonGenerator gen) throws IOException
    {
        gen.writeStartObject();
        gen.writeStringField("name", "Brad");
        gen.writeNumberField("age", 39);
        gen.writeEndObject();
        gen.close();
    }
}
