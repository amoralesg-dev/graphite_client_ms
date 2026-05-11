package com.rassini.graphite_client.service.xml.impl.util;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;

/**
 * Utilidades para fechas y horas usadas en XMLs (QAD).
 */
public final class DateUtil {

    private static final DateTimeFormatter DATE_DD_MM_YYYY =
            DateTimeFormatter.ofPattern("dd/MM/yyyy");

    private static final DateTimeFormatter TIME_HHMMSS =
            DateTimeFormatter.ofPattern("HHmmss");

    private DateUtil() {
        // utility class, no instances
    }

    /**
     * Fecha actual en formato dd/MM/yyyy
     * Ejemplo: 13/04/2026
     */
    public static String todayDdMmYyyy() {
        return LocalDate.now().format(DATE_DD_MM_YYYY);
    }

    /**
     * Hora actual en formato HHmmss
     * Ejemplo: 142305
     */
    public static String nowHhMmSs() {
        return LocalTime.now().format(TIME_HHMMSS);
    }

    
    /**
     * Fecha actual en formato yyyy-M-d
     * Ejemplo: 2026-4-13
     */
    public static String todayYyyyMD() {
        return LocalDate.now().getYear() + "-"
            + LocalDate.now().getMonthValue() + "-"
            + LocalDate.now().getDayOfMonth();
    }

}