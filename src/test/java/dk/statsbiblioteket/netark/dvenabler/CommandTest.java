/*
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 */
package dk.statsbiblioteket.netark.dvenabler;

import junit.framework.TestCase;
import org.apache.lucene.queryparser.classic.ParseException;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;

/**
 *
 */
public class CommandTest extends TestCase {

    public void testDump() throws IOException {
        final File INDEX = DVReaderTest.generateIndex();
        try {
            Command.main(new String[]{"-v", "-l", "-i", INDEX.toString()});
        } finally {
            DVReaderTest.delete(INDEX);
        }
    }

    public void testConvert() throws IOException, ParseException {
        final File IN = DVReaderTest.generateIndex();
        final File OUT = new File("target/testindex.deletefreely2");
        final String F_L = "long(NUMERIC(LONG))";
        final String F_M = "multistring(SORTED_SET)";
        final String F_S = "singlestring(SORTED)";
        try {
            Command.main(new String[]{"-v", "-c", "-i", IN.toString(), "-o", OUT.toString(), "-f", F_L, F_S, F_M});
            DVReaderTest.assertIndexValues(IN, false);
            DVReaderTest.assertIndexValues(OUT, true);
        } finally {
            DVReaderTest.delete(IN);
            DVReaderTest.delete(OUT);
        }
    }
}
