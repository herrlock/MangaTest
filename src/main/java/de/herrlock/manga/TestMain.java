package de.herrlock.manga;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.Method;
import java.nio.file.DirectoryStream;
import java.nio.file.DirectoryStream.Filter;
import java.nio.file.FileVisitResult;
import java.nio.file.FileVisitor;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.zip.ZipEntry;
import java.util.zip.ZipException;
import java.util.zip.ZipFile;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.google.common.base.Predicate;
import com.google.common.collect.Iterables;
import com.google.common.io.ByteStreams;

public class TestMain {
    private static final Logger logger = LogManager.getLogger();
    public static final Path temp = Paths.get( ".", "temp" );
    public static final Path out = temp.resolve( "out.txt" );
    public static final Path err = temp.resolve( "err.txt" );

    public static void main( final String... args ) throws Exception {
        Path workingDir = prepare();
        logger.info( "WorkingDir: {}", workingDir );
        runActions( workingDir );
        teardown();
    }

    private static Path prepare() throws IOException {
        logger.traceEntry();
        checkTempFolder();
        cleanStreamFiles();
        ZipFile zip = getZip();
        return unpackZip( zip );
    }

    private static void runActions( final Path workingDir ) {
        logger.traceEntry();
        List<Method> methods = new ArrayList<>( Arrays.asList( Actions.class.getDeclaredMethods() ) );
        Iterables.removeIf( methods, new Predicate<Method>() {
            @Override
            public boolean apply( Method input ) {
                return input.getAnnotation( TestIndex.class ) == null;
            }
        } );
        Collections.sort( methods, new Comparator<Method>() {
            @Override
            public int compare( Method m1, Method m2 ) {
                TestIndex ti1 = Objects.requireNonNull( m1.getAnnotation( TestIndex.class ) );
                TestIndex ti2 = Objects.requireNonNull( m2.getAnnotation( TestIndex.class ) );
                return Integer.compare( ti1.value(), ti2.value() );
            }
        } );
        String proxy = checkProxy();
        Actions a = new Actions( workingDir, proxy );
        for ( Method method : methods ) {
            try {
                method.invoke( a );
            } catch ( ReflectiveOperationException ex ) {
                logger.error( ex );
            }
        }
    }

    private static void teardown() {
        logger.traceEntry();
        cleanTemp();
        logger.info( "cleaned up" );
    }

    private static void checkTempFolder() throws IOException {
        if ( Files.notExists( temp ) ) {
            throw new NoSuchFileException( "./temp does not exist" );
        }
    }

    private static String checkProxy() {
        logger.traceEntry();
        return System.getenv( "http_proxy" );
    }

    private static void cleanStreamFiles() throws IOException {
        logger.traceEntry();
        try ( OutputStream xOut = Files.newOutputStream( out ) ) {
            // do nothing
        }
        try ( OutputStream xErr = Files.newOutputStream( err ) ) {
            // do nothing
        }
        logger.info( "out and err cleaned" );
    }

    private static ZipFile getZip() throws ZipException, IOException {
        logger.traceEntry();
        Path zip;
        try ( DirectoryStream<Path> directoryStream = Files.newDirectoryStream( temp, MANGA_ZIP_FILTER ) ) {
            zip = Iterables.getOnlyElement( directoryStream );
        }
        logger.info( "Zip to extract: {}", zip );
        return new ZipFile( zip.toFile() );
    }

    private static Path unpackZip( final ZipFile zip ) throws IOException {
        logger.traceEntry( "zip: {}", zip );
        String zipName = zip.getName();
        logger.debug( "Zip-name: {} ", zipName );
        Path extractTo = Paths.get( zipName.substring( 0, zipName.length() - 4 ) );
        logger.info( "ExtractTo: {}", extractTo );

        /* clean folder and recreate it */ {
            Files.walkFileTree( extractTo, DELETE_FILES_VISITOR );
            Files.createDirectories( extractTo );
        }

        /* extract zip */ {
            List<? extends ZipEntry> list = Collections.list( zip.entries() );
            logger.debug( "zip-entries: {}", list );
            for ( ZipEntry zipEntry : list ) {
                logger.debug( "  Entry: {}", zipEntry );
                Path extractToFile = extractTo.resolve( zipEntry.getName() );
                // File extractToFile = new File( extractTo, zipEntry.getName() );
                logger.debug( "  ExtractToFile: {}", extractToFile );
                if ( zipEntry.isDirectory() ) {
                    logger.debug( "    isDirectory => create dir" );
                    Files.createDirectories( extractToFile );
                    // extractToFile.mkdirs();
                } else {
                    logger.debug( "    not isDirectory => copy data to new file" );
                    try ( InputStream from = zip.getInputStream( zipEntry ) ) {
                        try ( OutputStream to = Files.newOutputStream( extractToFile ) ) {
                            ByteStreams.copy( from, to );
                        }
                    }
                }
            }
        }
        logger.info( "extracted zip" );

        try ( DirectoryStream<Path> directoryStream = Files.newDirectoryStream( extractTo ) ) {
            return Iterables.getOnlyElement( directoryStream );
        }
    }

    private static void cleanTemp() {
        logger.traceEntry();

    }

    private static final Filter<Path> MANGA_ZIP_FILTER = new Filter<Path>() {
        @Override
        public boolean accept( final Path entry ) throws IOException {
            Path fileName = entry.getFileName();
            return fileName != null && fileName.toString().matches( "^Manga-.+\\.zip$" );
        }
    };

    private static final FileVisitor<Path> DELETE_FILES_VISITOR = new FileVisitor<Path>() {
        @Override
        public FileVisitResult preVisitDirectory( Path dir, BasicFileAttributes attrs ) throws IOException {
            return FileVisitResult.CONTINUE;
        }

        @Override
        public FileVisitResult visitFile( Path file, BasicFileAttributes attrs ) throws IOException {
            Files.deleteIfExists( file );
            return FileVisitResult.CONTINUE;
        }

        @Override
        public FileVisitResult visitFileFailed( Path file, IOException exc ) throws IOException {
            return FileVisitResult.CONTINUE;
        }

        @Override
        public FileVisitResult postVisitDirectory( Path dir, IOException exc ) throws IOException {
            Files.deleteIfExists( dir );
            return FileVisitResult.CONTINUE;
        }
    };

}
