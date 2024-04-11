package com.unitbv.myquiz.services;

import com.unitbv.myquiz.repositories.QuestionRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.logging.Logger;

@Service
public class EncodingSeviceImpl implements EncodingSevice {

    Logger logger = Logger.getLogger(EncodingSeviceImpl.class.getName());

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
        logger.info("Server encoding: " + result);
        if (!result.equals("UTF8")) {
            logger.info("Please switch your server encoding to UTF8. \n" +
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
        if (enc.equals("ISO-8859-1")) {
            value = new String(value.getBytes(StandardCharsets.ISO_8859_1), StandardCharsets.UTF_8);
            System.out.println(">>> text converted to UTF_8: " + value);
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
            Charset charset = Charset.forName("UTF-8");
            if (Charset.forName("UTF-8").newDecoder().decode(ByteBuffer.wrap(bytes)).toString().equals(input)) {
                result = "UTF-8";
            } else if (Charset.forName("UTF-16").newDecoder().decode(ByteBuffer.wrap(bytes)).toString().equals(input)) {
                result = "UTF-16";
            } else if (Charset.forName("ISO-8859-1").newDecoder().decode(ByteBuffer.wrap(bytes)).toString().equals(input)) {
                result = "ISO-8859-1";
            } else {
                System.out.println("Cannot detect encoding of string: " + input);
            }
        } catch (Exception exception) {
            System.out.println("Exception when detecting encoding of string: " + input);
        }
        return result;
    }
}
