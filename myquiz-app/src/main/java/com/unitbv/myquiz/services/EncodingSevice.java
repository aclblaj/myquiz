package com.unitbv.myquiz.services;

public interface EncodingSevice {
    String detectEncoding(String input) ;

    String getServerEncoding();

    boolean checkServerEncoding();

    String convertToUTF8(String value);
}
