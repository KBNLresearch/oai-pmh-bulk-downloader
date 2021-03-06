package nl.kb.core.objectharvester;

import com.google.common.collect.Lists;
import nl.kb.core.model.reporting.ErrorReport;
import nl.kb.filestorage.FileStorageHandle;
import nl.kb.http.HttpFetcher;
import nl.kb.http.HttpResponseHandler;
import nl.kb.http.responsehandlers.ResponseHandlerFactory;
import nl.kb.manifest.ObjectResource;
import nl.kb.stream.ByteCountOutputStream;
import nl.kb.stream.ChecksumOutputStream;
import org.apache.commons.io.FilenameUtils;

import java.io.IOException;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.security.NoSuchAlgorithmException;
import java.util.List;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Stream;

import static java.util.stream.Collectors.toList;

public class ObjectHarvesterResourceOperations {
    private final HttpFetcher httpFetcher;
    private final ResponseHandlerFactory responseHandlerFactory;
    private final Function<String, String> createFilename;

    public ObjectHarvesterResourceOperations(HttpFetcher httpFetcher, ResponseHandlerFactory responseHandlerFactory) {
        this(httpFetcher, responseHandlerFactory, fileLocation -> UUID.randomUUID().toString());
    }

    ObjectHarvesterResourceOperations(HttpFetcher httpFetcher, ResponseHandlerFactory responseHandlerFactory,
                                      Function<String, String> createFilename) {
        this.httpFetcher = httpFetcher;
        this.responseHandlerFactory = responseHandlerFactory;
        this.createFilename = createFilename;
    }

    List<ErrorReport> downloadResource(
            ObjectResource objectResource,
            FileStorageHandle processingStorageHandle) throws IOException, NoSuchAlgorithmException {

        final String fileLocation = objectResource.getXlinkHref();
        final String filename = createFilename.apply(fileLocation);

        final OutputStream objectOut = processingStorageHandle.getOutputStream("resources", filename);
        final ChecksumOutputStream checksumOut = new ChecksumOutputStream("SHA-512");
        final ByteCountOutputStream byteCountOut = new ByteCountOutputStream();
        final ContentDispositionReader contentDispositionReader = new ContentDispositionReader();

        // First try to fetch the resource with the URL as is
        final List<ErrorReport> firstAttemptErrors = attemptDownload(objectOut, checksumOut, byteCountOut,
                contentDispositionReader, fileLocation);

        if (firstAttemptErrors.isEmpty()) {
            writeChecksumAndFilename(objectResource, checksumOut, byteCountOut, contentDispositionReader, filename);
            return Lists.newArrayList();
        }

        // Secondly try to fetch the resource by encoding the url name one way (whitespace as '+')
        final String preparedUrlWithPluses = prepareUrl(fileLocation, false);
        final List<ErrorReport> secondAttemptErrors = attemptDownload(objectOut, checksumOut, byteCountOut,
                contentDispositionReader, preparedUrlWithPluses);

        if (secondAttemptErrors.isEmpty()) {
            writeChecksumAndFilename(objectResource, checksumOut, byteCountOut, contentDispositionReader, filename);
            return Lists.newArrayList();
        }

        // Then try to fetch the resource by encoding the url name another way (whitespace as '%20')
        final String preparedUrlWithPercents = prepareUrl(fileLocation, true);
        if (preparedUrlWithPercents.equals(preparedUrlWithPluses)) {
            return secondAttemptErrors;
        }

        final List<ErrorReport> thirdAttemptErrors = attemptDownload(objectOut, checksumOut, byteCountOut,
                contentDispositionReader, preparedUrlWithPercents);

        if (thirdAttemptErrors.isEmpty()) {
            writeChecksumAndFilename(objectResource, checksumOut, byteCountOut, contentDispositionReader, filename);
            return Lists.newArrayList();
        }

        return Stream.concat(firstAttemptErrors.stream(), Stream
                .concat(secondAttemptErrors.stream(), thirdAttemptErrors.stream()))
                .collect(toList());
    }

    private void writeChecksumAndFilename(ObjectResource objectResource,
                                          ChecksumOutputStream checksumOut,
                                          ByteCountOutputStream byteCountOut,
                                          ContentDispositionReader contentDispositionReader,
                                          String filename)  {

        objectResource.setChecksum(checksumOut.getChecksumString());
        objectResource.setChecksumType("SHA-512");
        objectResource.setLocalFilename(filename);
        objectResource.setSize(byteCountOut.getCurrentByteCount());
        objectResource.setContentDisposition(contentDispositionReader.getContentDisposition());
        objectResource.setContentType(contentDispositionReader.getContentType());
    }

    private List<ErrorReport> attemptDownload(OutputStream objectOut, OutputStream checksumOut,
                                              ByteCountOutputStream byteCountOut,
                                              ContentDispositionReader contentDispositionReader,
                                              String preparedUrl) throws MalformedURLException {
        final HttpResponseHandler responseHandler = responseHandlerFactory
                .getStreamCopyingResponseHandler(objectOut, checksumOut, byteCountOut, contentDispositionReader);

        final URL objectUrl = new URL(preparedUrl);

        httpFetcher.execute(objectUrl, responseHandler);

        return ErrorReport.fromExceptionList(responseHandler.getExceptions());
    }

    private String prepareUrl(String rawUrl, boolean plusToPercent) throws UnsupportedEncodingException {
        final String name = FilenameUtils.getName(rawUrl);
        final String path = FilenameUtils.getPath(rawUrl);

        return path + encodeName(name, plusToPercent);
    }

    private String encodeName(String name, boolean plusToPercent) throws UnsupportedEncodingException {
        final String encodedName = name.equals(URLDecoder.decode(name, StandardCharsets.UTF_8.name()))
                ? URLEncoder.encode(name, StandardCharsets.UTF_8.name())
                : URLEncoder.encode(URLDecoder.decode(name, StandardCharsets.UTF_8.name()), StandardCharsets.UTF_8.name());

        return plusToPercent
                ? encodedName.replaceAll("\\+", "%20")
                : encodedName;
    }
}
