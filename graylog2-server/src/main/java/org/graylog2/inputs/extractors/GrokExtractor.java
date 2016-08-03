/**
 * This file is part of Graylog.
 *
 * Graylog is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Graylog is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Graylog.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.graylog2.inputs.extractors;

import com.codahale.metrics.MetricRegistry;
import com.google.common.base.Strings;
import com.google.common.collect.Lists;
import org.graylog2.ConfigurationException;
import org.graylog2.grok.GrokMatch;
import org.graylog2.grok.GrokPattern;
import org.graylog2.plugin.inputs.Converter;
import org.graylog2.plugin.inputs.Extractor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

public class GrokExtractor extends Extractor {
    private static final Logger log = LoggerFactory.getLogger(GrokExtractor.class);
    private final org.graylog2.grok.Grok grok;

//    private final Grok grok = new Grok();

    public GrokExtractor(MetricRegistry metricRegistry,
                         Set<GrokPattern> grokPatterns,
                         String id,
                         String title,
                         long order,
                         CursorStrategy cursorStrategy,
                         String sourceField,
                         String targetField,
                         Map<String, Object> extractorConfig,
                         String creatorUserId,
                         List<Converter> converters,
                         ConditionType conditionType,
                         String conditionValue) throws ReservedFieldException, ConfigurationException {
        super(metricRegistry,
              id,
              title,
              order,
              Type.GROK,
              cursorStrategy,
              sourceField,
              targetField,
              extractorConfig,
              creatorUserId,
              converters,
              conditionType,
              conditionValue);
        if (extractorConfig == null || Strings.isNullOrEmpty((String) extractorConfig.get("grok_pattern"))) {
            throw new ConfigurationException("grok_pattern not set");
        }

        final boolean namedCapturesOnly = (boolean) extractorConfig.getOrDefault("named_captures_only", false);

        try {
            // TODO we should really share this somehow, but unfortunately the extractors are reloaded every second.
//            for (final GrokPattern grokPattern : grokPatterns) {
//                grok.addPattern(grokPattern.name, grokPattern.pattern);
//            }

            final Map<String, String> patterns = grokPatterns.stream().collect(Collectors.toMap(GrokPattern::name,
                                                                                               GrokPattern::pattern));
            grok = new org.graylog2.grok.Grok(patterns, (String) extractorConfig.get("grok_pattern"), namedCapturesOnly);
//            grok.compile((String) extractorConfig.get("grok_pattern"), namedCapturesOnly);
        } catch (Exception e) {
            log.error("Unable to parse grok patterns", e);
            throw new ConfigurationException("Unable to parse grok patterns");
        }
    }

    @Override
    protected Result[] run(String value) {

        // the extractor instance is rebuilt every second anyway
        final GrokMatch match = grok.match(value);
//        match.captures();
        final Map<String, Object> matches = match.captures();
        final List<Result> results = Lists.newArrayListWithCapacity(matches.size());

        for (final Map.Entry<String, Object> entry : matches.entrySet()) {
            // never add null values to the results, those don't make sense for us
            if (entry.getValue() != null) {
                results.add(new Result(entry.getValue(), entry.getKey(), -1, -1));
            }
        }

        return results.toArray(new Result[results.size()]);
    }
}
