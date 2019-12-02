/*
 * ==========================License-Start=============================
 * DiscourseSimplification : App
 *
 * Copyright © 2017 Lambda³
 *
 * GNU General Public License 3
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see http://www.gnu.org/licenses/.
 * ==========================License-End==============================
 */

package org.lambda3.text.simplification.discourse;

import org.lambda3.text.simplification.discourse.processing.DiscourseSimplifier;
import org.lambda3.text.simplification.discourse.processing.ProcessingType;
import org.lambda3.text.simplification.discourse.model.SimplificationContent;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Scanner;
import java.util.concurrent.*;
import java.util.stream.Collectors;

public class App {
    private static final org.slf4j.Logger LOGGER = LoggerFactory.getLogger(App.class);
    //private static final DiscourseSimplifier DISCOURSE_SIMPLIFIER = new DiscourseSimplifier();
    private static ExecutorService executor = null;

    private static void saveLines(File file, List<String> lines) {
        try (BufferedWriter bw = new BufferedWriter(new FileWriter(file))) {
            bw.write(lines.stream().collect(Collectors.joining("\n")));

            // no need to close it.
            //bw.close()
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void main(String[] args) throws IOException, InterruptedException {
        SimplificationContent contents = new SimplificationContent();
        //SimplificationContent content = DISCOURSE_SIMPLIFIER.doDiscourseSimplification(new File("input.txt"), ProcessingType.SEPARATE, true);
        //SimplificationContent content = DISCOURSE_SIMPLIFIER.doDiscourseSimplification("Ram went to school and played football .", ProcessingType.SEPARATE);
        executor = Executors.newFixedThreadPool(12);
        int count=0;
        String line;

        Scanner scanner = new Scanner(new File("input.txt"));
        while (scanner.hasNextLine()) {
            LOGGER.info("processing line  " + count);
            count+=1;
            line=scanner.nextLine();
            line=line.replace("\n","");
            line=line.replace("'"," ");
            line=line.trim();
            if(count%50==0){
                Thread.sleep(5000);
            }
            String[] tokens = line.split("\t");
            if(tokens.length==2){
                executor.submit(new DiscourseSimplifier(tokens[0], ProcessingType.SEPARATE,tokens[1],contents));
            }
        }
        scanner.close();

        //executor.submit(new DiscourseSimplifier("Ram went to school and played football .", ProcessingType.SEPARATE,"Ram went to school . Ram played football . ",contents));
        //executor.submit(new DiscourseSimplifier("As she translates from one language to another , she tries to find the appropriate wording and context in English that would correspond to the work in Spanish her poems and stories started to have differing meanings in their respective languages .", ProcessingType.SEPARATE,"Ram went to school . Ram played football . ",contents));

        executor.shutdown();
        executor.awaitTermination(365, TimeUnit.DAYS);

        contents.serializeToJSON(new File("output.json"));
        saveLines(new File("output_default.txt"), Arrays.asList(contents.defaultFormat(false)));
        saveLines(new File("output_flat.txt"), Arrays.asList(contents.flatFormat(false)));
        LOGGER.info("done");
    }
}
