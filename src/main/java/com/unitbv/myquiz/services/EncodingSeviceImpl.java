package com.unitbv.myquiz.services;

import com.unitbv.myquiz.repositories.QuestionRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;

@Service
public class EncodingSeviceImpl implements EncodingSevice {

    public static final String UTF_8 = "UTF-8";
    public static final String UTF_16 = "UTF-16";
    public static final String ISO_8859_1 = "ISO-8859-1";
    Logger logger = LoggerFactory.getLogger(EncodingSeviceImpl.class.getName());

    @Autowired
    QuestionRepository questionRepository;

    @Override
    public String getServerEncoding() {
        String encoding = questionRepository.getEncoding();
        return encoding;
    }

    @Override
    public boolean checkServerEncoding() {
        String result = getServerEncoding();
        logger.atInfo().addArgument(result).log("Server encoding: {}");
        if (!result.equals("UTF8")) {
            logger.atInfo()
                  .log("Please switch your server encoding to UTF8. \n" +
                                "For example, after connecting to an SQL console, \n" +
                                "-- set encoding to UTF8\n" +
                                "SET client_encoding = 'UTF8';\n" +
                                "UPDATE pg_database SET datcollate='en_US.UTF-8', datctype='en_US.UTF-8' WHERE datname='postgres';\n" +
                                "UPDATE pg_database SET encoding = pg_char_to_encoding('UTF8') WHERE datname = 'myquiz' ;\n" +
                                "\n" +
                                "-- read encoding \n" +
                                "SELECT datname, pg_encoding_to_char(encoding) AS encoding FROM pg_database WHERE datname = 'myquiz'");
            return true;
        }
        return false;
    }

    @Override
    public String convertToUTF8(String value) {
        String enc = this.detectEncoding(value);
        if (enc.equals(ISO_8859_1)) {
            value = new String(value.getBytes(StandardCharsets.ISO_8859_1), StandardCharsets.UTF_8);
            logger.atInfo().addArgument(value).log("Text converted to UTF_8: {}");
        }
        return value;
    }

    public String detectEncoding(String input) {
        String result = "";
        byte[] bytes;
        try {
            bytes = input.getBytes(StandardCharsets.UTF_8);
        } catch (Exception exception) {
            bytes = input.getBytes(StandardCharsets.ISO_8859_1);
        }

        try {
            if (Charset.forName(UTF_8).newDecoder().decode(ByteBuffer.wrap(bytes)).toString().equals(input)) {
                result = UTF_8;
            } else if (Charset.forName(UTF_16).newDecoder().decode(ByteBuffer.wrap(bytes)).toString().equals(input)) {
                result = UTF_16;
            } else if (Charset.forName(ISO_8859_1).newDecoder().decode(ByteBuffer.wrap(bytes)).toString().equals(input)) {
                result = ISO_8859_1;
            } else {
                logger.atInfo().addArgument(input).log("Cannot detect encoding of string: {}");
            }
        } catch (Exception exception) {
            logger.atError().addArgument(input).log("Exception when detecting encoding of string: {}", exception);
        }
        return result;
    }
}
