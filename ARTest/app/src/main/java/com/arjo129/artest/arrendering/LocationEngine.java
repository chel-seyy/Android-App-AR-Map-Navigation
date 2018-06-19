package com.arjo129.artest.arrendering;

import java.util.Date;

public interface LocationEngine {
    void start();
    boolean hasBetterLocation(Date date);
    double getLat();
    double getLong();
}
