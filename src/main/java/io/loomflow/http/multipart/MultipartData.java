package io.loomflow.http.multipart;

import java.util.*;

/**
 * Parsed multipart/form-data — holds text fields and uploaded files.
 */
public final class MultipartData {

    private final Map<String, String>       fields;
    private final Map<String, UploadedFile> files;

    public MultipartData(Map<String, String> fields, Map<String, UploadedFile> files) {
        this.fields = Collections.unmodifiableMap(fields);
        this.files  = Collections.unmodifiableMap(files);
    }

    /** Get a text form field by name. Returns null if not present. */
    public String field(String name) { return fields.get(name); }

    /** Get text field with default value. */
    public String field(String name, String defaultValue) {
        return fields.getOrDefault(name, defaultValue);
    }

    /** Get an uploaded file by field name. Returns null if not present. */
    public UploadedFile file(String name) { return files.get(name); }

    /** All text fields. */
    public Map<String, String> fields() { return fields; }

    /** All uploaded files. */
    public Collection<UploadedFile> files() { return files.values(); }

    public boolean hasField(String name) { return fields.containsKey(name); }
    public boolean hasFile(String name)  { return files.containsKey(name); }
}
