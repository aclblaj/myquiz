package com.unitbv.myquiz.services;

import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;

public interface EncodingSevice {
    String detectEncoding(String input) ;

    String getServerEncoding();

    boolean checkServerEncoding();

    String convertToUTF8(String value);
}
