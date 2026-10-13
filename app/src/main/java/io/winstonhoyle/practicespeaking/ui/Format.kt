package io.winstonhoyle.practicespeaking.ui

import java.text.DateFormat
import java.util.Date

/** Session dates in the device's locale, e.g. "Oct 13, 2026, 4:05 PM". */
fun formatSessionDate(millis: Long): String = DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.SHORT).format(Date(millis))

/** How long a screen's flows stay active after it stops being visible (survives rotation without re-querying). */
const val STOP_TIMEOUT_MILLIS = 5_000L
