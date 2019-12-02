/*
 * ==========================License-Start=============================
 * DiscourseSimplification : DiscourseSimplifier
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

package org.lambda3.text.simplification.discourse.processing;

import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import org.lambda3.text.simplification.discourse.model.Element;
import org.lambda3.text.simplification.discourse.model.OutSentence;
import org.lambda3.text.simplification.discourse.model.SimplificationContent;
import org.lambda3.text.simplification.discourse.runner.discourse_extraction.DiscourseExtractor;
import org.lambda3.text.simplification.discourse.runner.discourse_tree.DiscourseTreeCreator;
import org.lambda3.text.simplification.discourse.utils.ConfigUtils;
import org.lambda3.text.simplification.discourse.utils.parseTree.ParseTreeException;
import org.lambda3.text.simplification.discourse.utils.sentences.SentencesUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Optional;

/**
 *
 */
public class DiscourseSimplifier implements Runnable {
    private final DiscourseTreeCreator discourseTreeCreator;
    private final DiscourseExtractor discourseExtractor;

    private final Logger logger = LoggerFactory.getLogger(getClass());
    String text;
    ProcessingType type;
    SimplificationContent contents;
    String simplifiedSentence;

    public DiscourseSimplifier(Config config) {
        SentencePreprocessor preprocessor = new SentencePreprocessor(config);
        this.discourseTreeCreator = new DiscourseTreeCreator(config, preprocessor);
        this.discourseExtractor = new DiscourseExtractor(config);

        logger.debug("DiscourseSimplifier initialized");
        logger.debug("\n{}", ConfigUtils.prettyPrint(config));
    }

    public DiscourseSimplifier() {
        this(ConfigFactory.load().getConfig("discourse-simplification"));
    }

    public DiscourseSimplifier(String text, ProcessingType type, String simplifiedSentence, SimplificationContent contents) {
        this(ConfigFactory.load().getConfig("discourse-simplification"));
        this.text=text;
        this.type=type;
        this.simplifiedSentence=simplifiedSentence;
        this.contents=contents;
    }


    public SimplificationContent doDiscourseSimplification(List<String> text,ProcessingType type){
        return null;
    }


    public void doDiscourseSimplification(String text, ProcessingType type, String simplifiedSentence,SimplificationContent content) {
        List<String> sentences = SentencesUtils.splitIntoSentences(text);
        if (type.equals(ProcessingType.SEPARATE)) {
            processSeparate(sentences, simplifiedSentence, content);
        } else {
            throw new IllegalArgumentException("Unknown ProcessingType.");
        }
        return;
    }


    // creates discourse trees for each individual sentence (investigates intra-sentential relations only)
    private void processSeparate(List<String> sentences,String simplifiedSentence, SimplificationContent content) {

        int idx = 0;
        for (String sentence : sentences) {
            OutSentence outSentence = new OutSentence(idx, sentence, simplifiedSentence);

            //logger.info("# Processing sentence {}/{} #", (idx + 1), sentences.size());
            logger.info("'" + sentence + "'");

            // Step 1) create sentence discourse tree
            logger.debug("### Step 1) CREATE SENTENCE DISCOURSE TREE ###");
            discourseTreeCreator.reset();
            try {
                logger.debug(sentence);
                discourseTreeCreator.addSentence(sentence, idx);
                discourseTreeCreator.update();
                logger.debug("tree is "+discourseTreeCreator.getDiscourseTree().toString());


                // Step 2) do discourse extraction
                logger.debug("### STEP 2) DO DISCOURSE EXTRACTION ###");
                List<Element> elements = discourseExtractor.doDiscourseExtraction(discourseTreeCreator.getDiscourseTree());
                elements.forEach(e -> outSentence.addElement(e));
                logger.debug(outSentence.toString());

            } catch (ParseTreeException e) {
                logger.error("Failed to process sentence: {}", sentence);
            }

            content.addSentence(outSentence);

            ++idx;
        }

        logger.info("### FINISHED");
        return;
    }

    @Override
    public void run() {
        doDiscourseSimplification(this.text,this.type,this.simplifiedSentence,this.contents);

    }
}
