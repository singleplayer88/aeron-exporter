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

import io.aeron.Aeron;
import io.aeron.ConcurrentPublication;
import io.aeron.Publication;
import io.aeron.Subscription;
import io.aeron.driver.MediaDriver;
import io.prometheus.client.Collector;
import org.agrona.concurrent.UnsafeBuffer;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.List;

import static java.lang.Thread.sleep;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AeronCollectorTest {

    public static final String CHANNEL = "aeron:udp?endpoint=localhost:24325";
    AeronCollector collector;

    @BeforeEach
    public void init() {
        collector = new AeronCollector(new CncFileReader());
    }

    @Test
    @DisplayName("Check that aeron counters will be correctly renamed for Prometheus")
    public void formatLabelName() {
        assertAll(
                () -> assertEquals("aeron_bytes_sent", collector.formatLabels("Bytes sent")),
                () -> assertEquals("aeron_failed_offers_to_receiverproxy", collector.formatLabels("Failed offers to ReceiverProxy")),
                () -> assertEquals("aeron_sender_flow_control_limits_ie_backpressure_events", collector.formatLabels("Sender flow control limits, i.e. back-pressure events")),
                () -> assertEquals("aeron_clientheartbeat_", collector.formatLabels("client-heartbeat: 1"))
        );
    }

    @Test
    public void shouldReturnErrorWhenFileNotFound() {
        List<Collector.MetricFamilySamples> mfs = collector.collect();
        assertTrue(mfs.stream().anyMatch(m -> "aeron_cncread_error".equals(m.name)));
    }

    @Test
    public void shouldReturnCollectionDurationWhenFileNotFound() {
        List<Collector.MetricFamilySamples> mfs = collector.collect();
        assertTrue(mfs.stream().anyMatch(m -> "aeron_exporter_duration_seconds".equals(m.name)), "Should return collection time even if collection was not successful");
    }

    @Test
    public void shouldReturnErrorOnFileReadingException(@Mock CncFileReader cncFileReader) throws Exception {
        //given
        when(cncFileReader.getCountersReader()).thenThrow(new IOException());
        collector = new AeronCollector(cncFileReader);
        //when
        List<Collector.MetricFamilySamples> mfs = collector.collect();
        //then
        assertTrue(mfs.stream().anyMatch(m -> "aeron_cncread_error".equals(m.name)));
    }

    @Test
    @Timeout(10)
    public void shouldReturnAllSystemCounters() throws Exception {
        //given
        MediaDriver driver = MediaDriver.launch(new MediaDriver.Context()
                .dirDeleteOnShutdown(true)
                .errorHandler(Throwable::printStackTrace));

        Aeron clientA = Aeron.connect();
        Aeron clientB = Aeron.connect();
        Subscription subscription = clientA.addSubscription(CHANNEL, 1);
        ConcurrentPublication publication = clientB.addPublication(CHANNEL, 1);

        UnsafeBuffer buffer = new UnsafeBuffer(new byte[256]);
        buffer.putBytes(0, "Message".getBytes());

        while (publication.offer(buffer) < 1L) {
            sleep(10);
        }

        //when
        List<Collector.MetricFamilySamples> mfs = collector.collect();
        if (mfs.size() == 0) {
            fail("No metrics returned");
        }
        List<Collector.MetricFamilySamples.Sample> samples = mfs.get(0).samples;

        //then
        assertAll(
                () -> assertTrue(samples.stream().anyMatch(m -> "aeron_bytes_sent".equals(m.name))),
                () -> assertTrue(samples.stream().anyMatch(m -> "aeron_bytes_received".equals(m.name))),
                () -> assertTrue(samples.stream().anyMatch(m -> "aeron_failed_offers_to_receiverproxy".equals(m.name))),
                () -> assertTrue(samples.stream().anyMatch(m -> "aeron_failed_offers_to_senderproxy".equals(m.name))),
                () -> assertTrue(samples.stream().anyMatch(m -> "aeron_failed_offers_to_driverconductorproxy".equals(m.name))),
                () -> assertTrue(samples.stream().anyMatch(m -> "aeron_naks_sent".equals(m.name))),
                () -> assertTrue(samples.stream().anyMatch(m -> "aeron_naks_received".equals(m.name))),
                () -> assertTrue(samples.stream().anyMatch(m -> "aeron_status_messages_sent".equals(m.name))),
                () -> assertTrue(samples.stream().anyMatch(m -> "aeron_status_messages_received".equals(m.name))),
                () -> assertTrue(samples.stream().anyMatch(m -> "aeron_heartbeats_sent".equals(m.name))),
                () -> assertTrue(samples.stream().anyMatch(m -> "aeron_heartbeats_received".equals(m.name))),
                () -> assertTrue(samples.stream().anyMatch(m -> "aeron_retransmits_sent".equals(m.name))),
                () -> assertTrue(samples.stream().anyMatch(m -> "aeron_flow_control_under_runs".equals(m.name))),
                () -> assertTrue(samples.stream().anyMatch(m -> "aeron_flow_control_over_runs".equals(m.name))),
                () -> assertTrue(samples.stream().anyMatch(m -> "aeron_invalid_packets".equals(m.name))),
                () -> assertTrue(samples.stream().anyMatch(m -> "aeron_errors".equals(m.name))),
                () -> assertTrue(samples.stream().anyMatch(m -> "aeron_short_sends".equals(m.name))),
                () -> assertTrue(samples.stream().anyMatch(m -> "aeron_failed_attempts_to_free_log_buffers".equals(m.name))),
                () -> assertTrue(samples.stream().anyMatch(m -> "aeron_sender_flow_control_limits_ie_backpressure_events".equals(m.name))),
                () -> assertTrue(samples.stream().anyMatch(m -> "aeron_unblocked_publications".equals(m.name))),
                () -> assertTrue(samples.stream().anyMatch(m -> "aeron_unblocked_control_commands".equals(m.name))),
                () -> assertTrue(samples.stream().anyMatch(m -> "aeron_possible_ttl_asymmetry".equals(m.name))),
                () -> assertTrue(samples.stream().anyMatch(m -> "aeron_controllableidlestrategy_status".equals(m.name))),
                () -> assertTrue(samples.stream().anyMatch(m -> "aeron_loss_gap_fills".equals(m.name))),
                () -> assertTrue(samples.stream().anyMatch(m -> "aeron_client_liveness_timeouts".equals(m.name))),
                () -> assertTrue(samples.stream().anyMatch(m -> "aeron_resolution_changes".equals(m.name)))
        );

        clientA.close();
        clientB.close();
        driver.close();
    }

    @Test
    @DisplayName("Generate temp cnc file in a default folder /dev/shm/aeron-{user}/cnc.dat")
    @Disabled
    public void generateCncFile() throws Exception {
        MediaDriver driver = MediaDriver.launch();
        Aeron aeron = Aeron.connect();
        Publication publication = aeron.addPublication(CHANNEL, 1);
        Subscription subscription = aeron.addSubscription(CHANNEL, 1);
        UnsafeBuffer buffer = new UnsafeBuffer(ByteBuffer.allocateDirect(256));
        buffer.putBytes(0, "Message".getBytes());
        while (true) {
            publication.offer(buffer);
            sleep(10);
        }
    }


}
