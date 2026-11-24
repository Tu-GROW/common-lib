package com.iris.common.lib.ref;

import java.time.LocalDate;

/** Durably reserves [start, start+size) of the day's counter and returns start. Must never hand out a range twice. */
public interface BlockAllocator {
    long allocate(LocalDate day, long size);
}
