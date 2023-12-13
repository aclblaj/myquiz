package com.unitbv.myquiz.services;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class MyUtil {
    // array list with forbidden titles
    public static final List<String> forbiddenTitles = List.of("Kapazität Null", "Kommunikationsmuster", "ICMP", "Roluri 2PC", "IP header", "c02_scheduler_functie");

    // error messages
    public static final String MISSING_POINTS = "Missing points - empty value for points";
    public static final String MISSING_TITLE = "Missing title";
    public static final String MISSING_ANSWER = "Missing answer";
    public static final String NOT_NUMERIC_COLUMN = "Conversion error - column contains no numeric values";
    public static final String DATATYPE_ERROR = "Datatype error";
    public static final String REFORMULATE_QUESTION_ANSWER_ALREADY_EXISTS = "Answer already exists";
    public static final String REFORMULATE_QUESTION_TITLE_ALREADY_EXISTS = "Title already exists";

    public static final String SKIPPED_DUE_TO_ERROR = "Skipped due to error";
    public static final String TEMPLATE_ERROR_1_4_POINTS_WRONG = "Template error - wrong sum for 1/4 points";
    public static final String TEMPLATE_ERROR_2_4_POINTS_WRONG = "Template error - wrong sum for 2/4 points";
    public static final String TEMPLATE_ERROR_3_4_POINTS_WRONG = "Template error - wrong sum for 3/4 points";
    public static final String TEMPLATE_ERROR_4_4_POINTS_WRONG = "Template error - wrong sum for 4/4 points";

    public static final String UNALLOWED_CHARS = "eroare salvare - caractere nepermise";
    public static final String EMPTY_QUESTION_TEXT = "eroare template - test intrebare este null";
    public static final String REMOVE_TEMPLATE_QUESTION = "eroare template - intrebare sablon - de sters: ";
    public static final String TITLE_NOT_STRING = "eroare template - questionTitle nu este string";
    public static final String MISSING_VALUES_LESS_THAN_11 = "eroare - valori lipsa, mai putin de 11 ";
    public static final String INCOMPLETE_ASSIGNEMENT_LESS_THAN_15_QUESTIONS = "tema incompleta - mai putin de 15 intrebari multiple choice";
}
