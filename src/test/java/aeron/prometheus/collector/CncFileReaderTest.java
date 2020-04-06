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
import io.aeron.driver.MediaDriver;
import org.agrona.concurrent.status.CountersReader;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.nio.file.Paths;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

class CncFileReaderTest {

    CncFileReader cncFileReader;

    @BeforeEach
    public void init() {
        cncFileReader = new CncFileReader();
    }

    @Test
    public void shouldFailForMissingCncFile() {
        System.setProperty("aeron.dir", Paths.get("./temp").toString());
        assertThrows(CncFileException.class, () -> cncFileReader.getCountersReader());
        System.getProperties().remove("aeron.dir");
    }

    @Test
    public void shouldReturnCncCountersForExistingFile() throws Exception {
        MediaDriver driver = MediaDriver.launch(new MediaDriver.Context()
                .dirDeleteOnShutdown(true)
                .errorHandler(Throwable::printStackTrace)
                .printConfigurationOnStart(true));

        CountersReader countersReader = cncFileReader.getCountersReader();

        assertNotNull(countersReader);

        driver.close();
    }

}
