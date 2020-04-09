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

import io.aeron.CncFileDescriptor;
import org.agrona.DirectBuffer;
import org.agrona.concurrent.status.CountersReader;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import static io.aeron.CommonContext.AERON_DIR_PROP_DEFAULT;
import static io.aeron.CommonContext.AERON_DIR_PROP_NAME;
import static java.lang.System.getProperty;

/**
 * Reads the cnc.dat file and exposes the counters with the help of org.agrona.concurrent.status.CountersReader, similar to io.aeron.samples.CncFileReader.
 * <p>
 * Uses the default aeron directory, otherwise use "aeron.dir" to change it.
 * Reads and maps counters file on every execution.
 */
public class CncFileReader {

    /**
     * Read the cnc.dat file from the aeron directory and exposes org.agrona.concurrent.status.CountersReader to iterate through the counters.
     *
     * @return CountersReader ready to iterate though the counters.
     * @throws FileNotFoundException when cnc.dat was not found.
     * @throws IOException           exception when reading the file.
     */
    public CountersReader getCountersReader() throws IOException {
        Path cncFilePath =
                Paths.get(
                        getProperty(AERON_DIR_PROP_NAME, AERON_DIR_PROP_DEFAULT), CncFileDescriptor.CNC_FILE);

        if (Files.notExists(cncFilePath)) {
            throw new FileNotFoundException("CnC file not found : " + cncFilePath.toString());
        }

        try (FileChannel fc = new RandomAccessFile(cncFilePath.toString(), "r").getChannel()) {
            final ByteBuffer cncByteBuffer = fc.map(FileChannel.MapMode.READ_ONLY, 0, fc.size());

            DirectBuffer cncMetaData = CncFileDescriptor.createMetaDataBuffer(cncByteBuffer);
            int cncVersion = cncMetaData.getInt(CncFileDescriptor.cncVersionOffset(0));

            CncFileDescriptor.checkVersion(cncVersion);

            return new CountersReader(
                    CncFileDescriptor.createCountersMetaDataBuffer(cncByteBuffer, cncMetaData),
                    CncFileDescriptor.createCountersValuesBuffer(cncByteBuffer, cncMetaData));
        }

    }

}
