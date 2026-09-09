/*
 * qTrace — QuPath workflow provenance extension
 * Copyright (C) 2026 Romain Tourte
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <https://www.gnu.org/licenses/>.
 *
 */

package io.qtrace;

import java.time.Instant;

/**
 * Immutable snapshot of one use of QuPath's built-in "Measurement maps" dialog
 * (Analyze / View > Measurement maps — {@code Commands.createMeasurementMapDialog}).
 *
 * This is a pure live-display action: QuPath never pushes anything to
 * {@code imageData.getHistoryWorkflow()} for it, so it has no script equivalent and is
 * never replayable — it exists purely as a provenance record ("someone looked at
 * measurement X colored by colormap Y over range [min, max]").
 *
 * Created by ActionLogger.snapshotMeasurementMap(); consumed by QTraceExporter.
 */
public class MeasurementMapRecord {

    public final String  measurementName;
    public final String  colormapName;
    public final double  rangeMin;
    public final double  rangeMax;
    public final Instant timestamp;

    public MeasurementMapRecord(String measurementName, String colormapName,
                                 double rangeMin, double rangeMax, Instant timestamp) {
        this.measurementName = measurementName;
        this.colormapName    = colormapName;
        this.rangeMin        = rangeMin;
        this.rangeMax        = rangeMax;
        this.timestamp       = timestamp;
    }
}
