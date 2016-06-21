package de.herrlock.manga;

import static de.herrlock.manga.TestMain.err;
import static de.herrlock.manga.TestMain.out;

import java.io.IOException;
import java.lang.ProcessBuilder.Redirect;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.google.common.collect.Iterables;

public final class Actions {
    private static final Logger logger = LogManager.getLogger();
    private static final String[] basecommand = "java -jar MangaLauncher.jar".split( " " );
    private final Path wd;
    private final String proxy;

    public Actions( final Path wd, final String proxy ) {
        this.wd = wd;
        this.proxy = proxy == null ? null : ( proxy.startsWith( "http://" ) ? "" : "http://" ) + proxy;
    }

    private Process runProcess( final String... args ) throws IOException {
        logger.traceEntry( "Arguments: {}", Arrays.toString( args ) );
        String[] commands = new String[3 + args.length];
        System.arraycopy( basecommand, 0, commands, 0, 3 );
        System.arraycopy( args, 0, commands, 3, args.length );
        logger.info( "Commands: {}", Arrays.toString( commands ) );
        ProcessBuilder pb = new ProcessBuilder( commands )//
            .redirectOutput( Redirect.appendTo( out.toFile() ) )//
            .redirectError( Redirect.appendTo( err.toFile() ) )//
            .directory( this.wd.toFile() );
        logger.debug( "ProcessBuilder: {} ", pb );
        logger.info( "Starting process" );
        Process process = pb.start();
        logger.debug( "Process: {}", process );
        return process;
    }

    private int runProcessAndWait( final String... args ) throws IOException, InterruptedException {
        logger.traceEntry( "Arguments: {}", Arrays.toString( args ) );
        Process process = runProcess( args );
        int exitCode = process.waitFor();
        logger.info( "Finished process." );
        if ( exitCode == 0 ) {
            logger.info( "Processes succeeded" );
        } else {
            logger.error( "Process failed with the error-code: {}", exitCode );
        }
        return exitCode;
    }

    private int runConsoleProcess( final String... args ) throws IOException, InterruptedException {
        if ( this.proxy == null ) {
            return runProcessAndWait( args );
        }
        String[] newArgs = new String[args.length + 2];
        System.arraycopy( args, 0, newArgs, 0, args.length );
        newArgs[newArgs.length - 2] = "-x";
        newArgs[newArgs.length - 1] = this.proxy;
        return runProcessAndWait( newArgs );
    }

    @TestIndex( 0 )
    public void runHelp() throws IOException, InterruptedException {
        runProcessAndWait( "--help" );
    }

    @TestIndex( 1 )
    public void runConsolePandaNaruto() throws IOException, InterruptedException {
        runConsoleProcess( "--console", //
            "--url", "http://mangapanda.com/naruto", //
            "--pattern", "3-10;695-700" );
    }

    @TestIndex( 2 )
    public void runViewpage() throws IOException, InterruptedException {
        Path justDownloadedNaruto;
        try ( DirectoryStream<Path> dirStream = Files.newDirectoryStream( this.wd.resolve( "download" ) ) ) {
            justDownloadedNaruto = Iterables.getFirst( dirStream, null );
        }
        runProcessAndWait( "--viewpage", //
            "--folder", justDownloadedNaruto.toAbsolutePath().toString() );
    }

    @TestIndex( 3 )
    public void runConsolePandaOnepiece() throws IOException, InterruptedException {
        runConsoleProcess( "--console", //
            "--url", "http://mangapanda.com/one-piece", //
            "--pattern", "2;62;512-514" );
    }

    @TestIndex( 4 )
    public void runConsoleFoxNaruto() throws IOException, InterruptedException {
        runConsoleProcess( "--console", //
            "--url", "http://mangafox.me/manga/naruto", //
            "--pattern", "42" );
    }
}
