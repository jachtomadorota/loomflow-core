package io.loomflow.http.multipart;

import io.loomflow.http.Request;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Minimal multipart/form-data parser — pure JDK, no external dependencies.
 * Supports text fields and file uploads. Max part size: limited by available heap.
 */
public final class MultipartParser {

    private MultipartParser() {}

    /**
     * Parse the request body as multipart/form-data.
     * The Content-Type header must contain the boundary parameter.
     *
     * @throws IllegalArgumentException if Content-Type is missing or not multipart
     * @throws IOException              on parse error
     */
    public static MultipartData parse(Request req) throws IOException {
        String contentType = req.header("content-type");
        if (contentType == null || !contentType.contains("multipart/form-data")) {
            throw new IllegalArgumentException(
                    "Expected multipart/form-data but got: " + contentType);
        }

        String boundary = extractBoundary(contentType);
        byte[] body = req.bodyBytes();
        return parse(body, boundary);
    }

    // ── Internal parser ────────────────────────────────────────────────────────

    static MultipartData parse(byte[] body, String boundary) throws IOException {
        Map<String, String>       fields = new LinkedHashMap<>();
        Map<String, UploadedFile> files  = new LinkedHashMap<>();

        byte[] delimiter    = ("--" + boundary).getBytes(StandardCharsets.ISO_8859_1);
        byte[] endDelimiter = ("--" + boundary + "--").getBytes(StandardCharsets.ISO_8859_1);

        int pos = indexOf(body, delimiter, 0);
        if (pos < 0) return new MultipartData(fields, files);

        while (true) {
            int partStart = pos + delimiter.length;
            // skip CRLF after boundary
            if (partStart < body.length && body[partStart] == '\r') partStart++;
            if (partStart < body.length && body[partStart] == '\n') partStart++;

            // check for end boundary
            if (startsWith(body, endDelimiter, pos)) break;

            int nextBoundary = indexOf(body, delimiter, partStart);
            if (nextBoundary < 0) break;

            // part bytes (strip trailing CRLF before next boundary)
            int partEnd = nextBoundary;
            if (partEnd > 0 && body[partEnd - 1] == '\n') partEnd--;
            if (partEnd > 0 && body[partEnd - 1] == '\r') partEnd--;

            // parse headers
            int headerEnd = indexOfCrLfCrLf(body, partStart);
            if (headerEnd < 0) { pos = nextBoundary; continue; }

            String headerBlock = new String(body, partStart, headerEnd - partStart,
                                            StandardCharsets.ISO_8859_1);
            Map<String, String> headers = parsePartHeaders(headerBlock);

            String disposition = headers.getOrDefault("content-disposition", "");
            String fieldName   = extractParam(disposition, "name");
            String fileName    = extractParam(disposition, "filename");

            int dataStart = headerEnd + 4; // skip \r\n\r\n
            byte[] data   = copy(body, dataStart, partEnd);

            if (fileName != null && !fileName.isEmpty()) {
                String partContentType = headers.getOrDefault("content-type",
                                                              "application/octet-stream");
                files.put(fieldName, new UploadedFile(fieldName, fileName,
                                                      partContentType.trim(), data));
            } else if (fieldName != null) {
                fields.put(fieldName, new String(data, StandardCharsets.UTF_8));
            }

            pos = nextBoundary;
        }

        return new MultipartData(fields, files);
    }

    // ── Helpers ────────────────────────────────────────────────────────────────

    private static String extractBoundary(String contentType) {
        for (String part : contentType.split(";")) {
            part = part.trim();
            if (part.startsWith("boundary=")) {
                String b = part.substring("boundary=".length()).trim();
                return b.startsWith("\"") ? b.substring(1, b.length() - 1) : b;
            }
        }
        throw new IllegalArgumentException("No boundary in Content-Type: " + contentType);
    }

    private static Map<String, String> parsePartHeaders(String block) {
        Map<String, String> headers = new LinkedHashMap<>();
        for (String line : block.split("\r\n")) {
            int colon = line.indexOf(':');
            if (colon > 0) {
                headers.put(line.substring(0, colon).trim().toLowerCase(),
                            line.substring(colon + 1).trim());
            }
        }
        return headers;
    }

    private static String extractParam(String header, String paramName) {
        String search = paramName + "=";
        int idx = header.indexOf(search);
        if (idx < 0) return null;
        int start = idx + search.length();
        if (start < header.length() && header.charAt(start) == '"') {
            int end = header.indexOf('"', start + 1);
            return end > start ? header.substring(start + 1, end) : null;
        }
        int end = header.indexOf(';', start);
        return end > 0 ? header.substring(start, end).trim()
                       : header.substring(start).trim();
    }

    private static int indexOf(byte[] data, byte[] pattern, int from) {
        outer:
        for (int i = from; i <= data.length - pattern.length; i++) {
            for (int j = 0; j < pattern.length; j++) {
                if (data[i + j] != pattern[j]) continue outer;
            }
            return i;
        }
        return -1;
    }

    private static boolean startsWith(byte[] data, byte[] pattern, int offset) {
        if (offset + pattern.length > data.length) return false;
        for (int i = 0; i < pattern.length; i++) {
            if (data[offset + i] != pattern[i]) return false;
        }
        return true;
    }

    private static int indexOfCrLfCrLf(byte[] data, int from) {
        for (int i = from; i <= data.length - 4; i++) {
            if (data[i]=='\r' && data[i+1]=='\n' && data[i+2]=='\r' && data[i+3]=='\n')
                return i;
        }
        return -1;
    }

    private static byte[] copy(byte[] src, int from, int to) {
        if (from >= to) return new byte[0];
        byte[] out = new byte[to - from];
        System.arraycopy(src, from, out, 0, out.length);
        return out;
    }
}
