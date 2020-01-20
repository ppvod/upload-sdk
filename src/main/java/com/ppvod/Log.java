package com.ppvod;

import org.slf4j.LoggerFactory;

import org.slf4j.Logger;

public class Log {
    private Logger logger;

    public Log(String name) {
        this.logger = LoggerFactory.getLogger(name);
    }

    public static Log getLogger(String logger) {
        return new Log(logger);
    }

    public void info(String s) {
        logger.info(s);
    }

    public void error(String s) {
        logger.error(s);
    }

    public void debug(String s) {

        logger.debug(s);
    }
}