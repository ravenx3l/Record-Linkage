package org.jembi.jempi.linker.thresholdRangeProcessor.standardRangeProcessor.processors.FieldEqualityPairMatchProcessor;

import org.jembi.jempi.AppConfig;
import org.jembi.jempi.libmpi.LibMPI;
import org.jembi.jempi.libmpi.LibMPIClientInterface;
import org.jembi.jempi.linker.backend.LinkerDWH;
import org.jembi.jempi.linker.backend.LinkerUtils;
import org.jembi.jempi.linker.backend.CustomLinkerDeterministic;
import org.jembi.jempi.linker.backend.CustomLinkerProbabilistic;
import org.jembi.jempi.linker.thresholdRangeProcessor.lib.rangeType.RangeDetails;
import org.jembi.jempi.linker.thresholdRangeProcessor.lib.rangeType.RangeTypeFactory;
import org.jembi.jempi.linker.thresholdRangeProcessor.standardRangeProcessor.StandardThresholdRangeProcessor;
import org.jembi.jempi.linker.thresholdRangeProcessor.utls.MockInteractionCreator;
import org.jembi.jempi.linker.thresholdRangeProcessor.utls.MockLibMPI;
import org.jembi.jempi.shared.models.CustomDemographicData;
import org.jembi.jempi.shared.models.Interaction;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;
import static org.junit.jupiter.api.Assertions.assertEquals;
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class VerticalFieldEqualityPairMatchTests {
    LibMPI libMPI = null;
    @BeforeAll
    void setLibMPI(){
        libMPI = MockLibMPI.getLibMPI();
    }

    // Extract of org.jembi.jempi.linker.backend.LinkerDWH.linkInteraction
    public void mockLinkInteraction(final Interaction interaction) throws ExecutionException, InterruptedException {
        StandardThresholdRangeProcessor thresholdProcessor =
                (StandardThresholdRangeProcessor) new StandardThresholdRangeProcessor("linker", interaction).SetRanges(List.of(RangeTypeFactory.StandardThresholdAboveThreshold(AppConfig.LINKER_MATCH_THRESHOLD, 1.0F)));

        libMPI.startTransaction();
        final var candidateGoldenRecords = libMPI.findLinkCandidates(interaction.demographicData());

        if (candidateGoldenRecords.isEmpty()) {
             libMPI.createInteractionAndLinkToClonedGoldenRecord(interaction, 1.0F);
        } else {
            thresholdProcessor.ProcessCandidates(candidateGoldenRecords);

            final var allCandidateScores =
                    candidateGoldenRecords.parallelStream()
                            .unordered()
                            .map(candidate -> new LinkerDWH.WorkCandidate(candidate,
                                    LinkerUtils.calcNormalizedScore(candidate.demographicData(),
                                            interaction.demographicData())))
                            .sorted((o1, o2) -> Float.compare(o2.score(), o1.score()))
                            .collect(Collectors.toCollection(ArrayList::new));

            final var candidatesAboveMatchThreshold =
                    allCandidateScores
                            .stream()
                            .filter(v -> v.score() >= AppConfig.LINKER_MATCH_THRESHOLD)
                            .collect(Collectors.toCollection(ArrayList::new));


            if (candidatesAboveMatchThreshold.isEmpty()) {
                libMPI.createInteractionAndLinkToClonedGoldenRecord(interaction, 1.0F);
            }
            else{
                final var firstCandidate = candidatesAboveMatchThreshold.get(0);
                final var linkToGoldenId =
                        new LibMPIClientInterface.GoldenIdScore(firstCandidate.goldenRecord().goldenId(), firstCandidate.score());
                final var validated1 =
                        CustomLinkerDeterministic.validateDeterministicMatch(firstCandidate.goldenRecord().demographicData(),
                                interaction.demographicData());
                final var validated2 =
                        CustomLinkerProbabilistic.validateProbabilisticScore(firstCandidate.goldenRecord().demographicData(),
                                interaction.demographicData());

                        libMPI.createInteractionAndLinkToExistingGoldenRecord(interaction,
                                linkToGoldenId,
                                validated1,
                                validated2);
            }
        }
    }

    public void useCSVFile(String csvFile) throws ExecutionException, InterruptedException {
            try {
                InputStream inputStream = VerticalFieldEqualityPairMatchTests.class.getResourceAsStream(csvFile);
                CSVParser csvParser = CSVFormat.DEFAULT.withHeader().parse(new InputStreamReader(inputStream));

                for (CSVRecord record : csvParser) {
                    this.mockLinkInteraction(MockInteractionCreator.interactionFromDemographicData(null,
                            new CustomDemographicData(
                                    record.get("givenName"),
                                    record.get("familyName"),
                                    record.get("gender"),
                                    record.get("dob"),
                                    record.get("city"),
                                    record.get("phoneNumber"),
                                    record.get("nationalId")
                            )
                    ));
                }

                csvParser.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
    }

    @Test
    void testGetMandUFor10Records() {
        try{
            libMPI.startTransaction();
            libMPI.dropAll(); libMPI.createSchema();
            useCSVFile("/csv/basic.csv");
        } catch (Exception e){
            System.out.println(e.getMessage());
            e.printStackTrace();
        }

    }
}