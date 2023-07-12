package com.bmv.heartrate;

import java.util.Date;

public class Measurement<T> {
    public final Date timestamp;
    public final T measurement;

    public Measurement(Date timestamp, T measurement) {
        this.timestamp = timestamp;
        this.measurement = measurement;
    }
}
