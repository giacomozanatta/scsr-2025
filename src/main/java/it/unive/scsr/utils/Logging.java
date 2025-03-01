package it.unive.scsr.utils;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.logging.*;

public class Logging {
    public static final Logger defaultLogger = buildDefaultLogger();

    private static Logger buildDefaultLogger() {
        final var logger = Logger.getLogger(Logging.class.getName());
        logger.setUseParentHandlers(false);
        logger.addHandler(new StreamHandler(System.out, new Formatter() {
            @Override
            public String format(LogRecord record) {
                return new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date(record.getMillis())) +
                        " [" + record.getLongThreadID() + "] " +
                        record.getLevel() + " " +
                        record.getLoggerName() + " - " +
                        formatMessage(record) + "\n";
            }
        }));

        return logger;
    }
}
