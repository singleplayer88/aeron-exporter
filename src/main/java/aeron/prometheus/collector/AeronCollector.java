/*
 * Copyright (c) 2020.
 *   Licensed under the Apache License, Version 2.0 (the "License");
 *   you may not use this file except in compliance with the License.
 *   You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *   Unless required by applicable law or agreed to in writing, software
 *   distributed under the License is distributed on an "AS IS" BASIS,
 *   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *   See the License for the specific language governing permissions and
 *   limitations under the License.
 */

package aeron.prometheus.collector;

import io.prometheus.client.Collector;
import org.agrona.concurrent.status.CountersReader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import static io.aeron.driver.status.SystemCounterDescriptor.SYSTEM_COUNTER_TYPE_ID;

/**
 * Exports counters from aeron cnc.dat file to Prometheus.
 * <p>
 * Aeron counters file is expected at the default path, use "aeron.dir" property to change it.
 * Reads the systems counters from the cnc.dat file, sanitizes the labels to comply with <a href="https://prometheus.io/docs/practices/naming/">Prometheus naming conventions</a>.
 * Using the UNTYPED type for the Prometheus metrics according to the guide <a href="https://prometheus.io/docs/instrumenting/writing_exporters/">here</a>.
 */
public final class AeronCollector extends Collector implements Collector.Describable {
    private static final Logger LOGGER = LoggerFactory.getLogger(AeronCollector.class);

    private static final String COLLECTOR_DURATION_METRIC = "aeron_exporter_duration_seconds";
    private static final String AERON_PREFIX = "aeron_";
    private static final String AERON_CNCREAD_ERROR = "aeron_cncread_error";
    private static final String METRIC_NAME = "aeron";

    private final CncFileReader cncFileReader;

    public AeronCollector(CncFileReader cncFileReader) {
        this.cncFileReader = cncFileReader;
    }

    /**
     * Reads the counters from the cnc.dat file.
     *
     * @return list of metrics with system counters data or an error metric in case file read failed.
     */
    @Override
    public List<MetricFamilySamples> collect() {
        long start = System.nanoTime();

        var mfsList = new ArrayList<MetricFamilySamples>();
        var samples = new ArrayList<MetricFamilySamples.Sample>();

        try {
            addCountersToMetricList(mfsList, samples);

        } catch (IOException e) {
            LOGGER.atError().log("Error during cnc.dat read", e);

            List<MetricFamilySamples.Sample> error = new ArrayList<>();
            error.add(new MetricFamilySamples.Sample(
                    AERON_CNCREAD_ERROR, new ArrayList<>(), new ArrayList<>(), 1));
            mfsList.add(new MetricFamilySamples(AERON_CNCREAD_ERROR, Type.GAUGE, "Non-zero if cnc file read has failed.", error));
        } finally {
            //add a duration of how long did the collection take
            List<MetricFamilySamples.Sample> duration = new ArrayList<>();
            duration.add(new MetricFamilySamples.Sample(
                    COLLECTOR_DURATION_METRIC, new ArrayList<>(), new ArrayList<>(), (System.nanoTime() - start) / NANOSECONDS_PER_SECOND));
            mfsList.add(new MetricFamilySamples(COLLECTOR_DURATION_METRIC, Type.GAUGE, "Time aeron counters read took, in seconds.", duration));
        }
        return mfsList;
    }

    private void addCountersToMetricList(ArrayList<MetricFamilySamples> mfsList, ArrayList<MetricFamilySamples.Sample> samples) throws IOException {
        CountersReader countersReader = cncFileReader.getCountersReader();

        countersReader.forEach((counterId, typeId, directBuffer, label) -> {
            final long value = countersReader.getCounterValue(counterId);

            // include only the system counters
            if (typeId == SYSTEM_COUNTER_TYPE_ID) {
                String counterName = formatLabels(label);
                samples.add(new MetricFamilySamples.Sample(counterName, new ArrayList<>(), new ArrayList<>(), value));
            }
        });

        mfsList.add(new MetricFamilySamples(METRIC_NAME, Type.UNTYPED, "Aeron CNC system counters", samples));
    }

    /**
     * Sanitizes label name to match Prometheus naming standards.
     *
     * @param label counter label as stored by aeron in cnc.dat
     * @return sanitized name.
     */
    public String formatLabels(String label) {
        return AERON_PREFIX + label.toLowerCase(Locale.US).replace(" ", "_").replaceAll("[^A-Za-z_]", "");
    }

    @Override
    public List<MetricFamilySamples> describe() {
        List<MetricFamilySamples> metricFamilies = new ArrayList<>();
        metricFamilies.add(new MetricFamilySamples(COLLECTOR_DURATION_METRIC, Type.UNTYPED, "Time aeron counters read took, in seconds.", new ArrayList<>()));
        metricFamilies.add(new MetricFamilySamples(AERON_CNCREAD_ERROR, Type.GAUGE, "Non-zero if cnc file read has failed.", new ArrayList<>()));
        return metricFamilies;
    }
}
