package org.jembi.jempi.shared.models;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.util.List;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record GoldenRecord(
      String goldenId,
      List<SourceId> sourceId,
      CustomUniqueGoldenRecordData customUniqueGoldenRecordData,
      CustomDemographicData demographicData) {

   public GoldenRecord(final Interaction interaction) {
      this(null,
           List.of(interaction.sourceId()),
           new CustomUniqueGoldenRecordData(true,
                                            interaction.uniqueInteractionData().auxId()),
           interaction.demographicData());
   }

}
