package org.jembi.jempi.libmpi.dgraph;

import com.google.protobuf.ByteString;
import io.dgraph.DgraphProto;
import io.netty.util.internal.StringUtil;
import io.vavr.control.Either;
import io.vavr.control.Option;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jembi.jempi.libmpi.LibMPIClientInterface;
import org.jembi.jempi.libmpi.MpiGeneralError;
import org.jembi.jempi.libmpi.MpiServiceError;
import org.jembi.jempi.shared.models.*;
import org.jembi.jempi.shared.utils.AppUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

final class DgraphMutations {

   private static final Logger LOGGER = LogManager.getLogger(DgraphMutations.class);

   private DgraphMutations() {
   }

   private static String createSourceIdTriple(final SourceId sourceId) {
      final String uuid = UUID.randomUUID().toString();
      return String.format("""
                           _:%s  <SourceId.facility>                 %s          .
                           _:%s  <SourceId.patient>                  %s          .
                           _:%s  <dgraph.type>                      "SourceId"   .
                           """, uuid, AppUtils.quotedValue(sourceId.facility()), uuid, AppUtils.quotedValue(sourceId.patient()),
                           uuid);
   }

   private static DgraphSourceIds getSourceId(final SourceId sourceId) {
      if (StringUtils.isBlank(sourceId.facility())
          || StringUtils.isBlank(sourceId.patient())) {
         return new DgraphSourceIds(List.of());
      }
      final String query = String.format(
            """
            query query_source_id() {
               var(func: eq(SourceId.facility, "%s")) {
                  A as uid
               }
               var(func: eq(SourceId.patient, "%s")) {
                  B as uid
               }
               all(func: uid(A,B)) @filter (uid(A) AND uid(B)) {
                  uid
                  expand(SourceId)
               }
            }
            """, sourceId.facility(), sourceId.patient());
      return DgraphQueries.runSourceIdQuery(query);
   }

   private static boolean updateGoldenRecordPredicate(
         final String goldenId,
         final String predicate,
         final String value) {
      final var mutation = DgraphProto.Mutation.newBuilder()
                                               .setSetNquads(ByteString.copyFromUtf8(String.format(
                                                     """
                                                     <%s> <%s>          "%s"^^<xs:string>    .
                                                     <%s> <dgraph.type> "GoldenRecord"       .
                                                     """, goldenId, predicate, value, goldenId)))
                                               .build();
      final var result = DgraphClient.getInstance().doMutateTransaction(mutation);
      return StringUtil.isNullOrEmpty(result);
   }

   //Use this when checking auto-update
   private static boolean updateGoldenRecordPredicate(
         final String goldenId,
         final String predicate,
         final Boolean value) {
      final var mutation = DgraphProto.Mutation.newBuilder()
                                               .setSetNquads(ByteString.copyFromUtf8(String.format(
                                                     """
                                                     <%s> <%s>          "%s"^^<xs:boolean>   .
                                                     <%s> <dgraph.type> "GoldenRecord"       .
                                                     """,
                                                     goldenId,
                                                     predicate,
                                                     Boolean.TRUE.equals(value)
                                                           ? "true"
                                                           : "false",
                                                     goldenId)))
                                               .build();
      final var result = DgraphClient.getInstance().doMutateTransaction(mutation);
      return StringUtil.isNullOrEmpty(result);
   }

   private static boolean updateGoldenRecordPredicate(
         final String goldenId,
         final String predicate,
         final Double value) {
      final var mutation = DgraphProto.Mutation.newBuilder()
                                               .setSetNquads(ByteString.copyFromUtf8(String.format(
                                                     """
                                                     <%s> <%s>          "%f"^^<xs:double>    .
                                                     <%s> <dgraph.type> "GoldenRecord"       .
                                                     """, goldenId, predicate, value, goldenId)))
                                               .build();
      final var result = DgraphClient.getInstance().doMutateTransaction(mutation);
      return StringUtil.isNullOrEmpty(result);
   }

   private static boolean updateGoldenRecordPredicate(
         final String goldenId,
         final String predicate,
         final Long value) {
      final var mutation = DgraphProto.Mutation.newBuilder()
                                               .setSetNquads(ByteString.copyFromUtf8(String.format(
                                                     """
                                                     <%s> <%s>          "%d"^^<xs:integer>    .
                                                     <%s> <dgraph.type> "GoldenRecord"       .
                                                     """, goldenId, predicate, value, goldenId)))
                                               .build();
      final var result = DgraphClient.getInstance().doMutateTransaction(mutation);
      return StringUtil.isNullOrEmpty(result);
   }

   private static boolean deletePredicate(
         final String uid,
         final String predicate,
         final String value) {
      final var mutation = DgraphProto.Mutation.newBuilder()
                                               .setDelNquads(ByteString.copyFromUtf8(String.format(
                                                     """
                                                     <%s>  <%s>  <%s>  .
                                                     """, uid, predicate, value)))
                                               .build();
      final var result = DgraphClient.getInstance().doMutateTransaction(mutation);
      return result != null;
   }

   private static void addScoreFacets(final List<DgraphPairWithScore> interactionScoreList) {
      StringBuilder simWeightFacet = new StringBuilder();
      for (DgraphPairWithScore interactionScore : interactionScoreList) {
         simWeightFacet.append(
               String.format("<%s> <GoldenRecord.interactions> <%s> (score=%f) .%n",
                             interactionScore.goldenUID(), interactionScore.interactionUID(), interactionScore.score()));
      }

      final var s = simWeightFacet.toString();
      final DgraphProto.Mutation mu = DgraphProto.Mutation.newBuilder().setSetNquads(ByteString.copyFromUtf8(s))
                                                          .build();

      DgraphClient.getInstance().doMutateTransaction(mu);
   }

   private static void addSourceId(
         final String uid,
         final String sourceId) {
      var mutation = String.format("<%s> <GoldenRecord.source_id> <%s> .%n", uid, sourceId);
      final DgraphProto.Mutation mu = DgraphProto.Mutation.newBuilder().setSetNquads(ByteString.copyFromUtf8(mutation))
                                                          .build();
      DgraphClient.getInstance().doMutateTransaction(mu);
   }

   private static InsertInteractionResult insertInteraction(final Interaction interaction) {
      final DgraphProto.Mutation sourceIdMutation = DgraphProto.Mutation.newBuilder()
                                                                        .setSetNquads(ByteString.copyFromUtf8(createSourceIdTriple(
                                                                              interaction.sourceId())))
                                                                        .build();
      final var sourceId = getSourceId(interaction.sourceId()).all();
      final var sourceIdUid = !sourceId.isEmpty()
            ? sourceId.get(0).uid()
            : DgraphClient.getInstance().doMutateTransaction(sourceIdMutation);
      final DgraphProto.Mutation mutation = DgraphProto
            .Mutation
            .newBuilder()
            .setSetNquads(ByteString.copyFromUtf8(CustomDgraphMutations
                                                        .createInteractionTriple(interaction.uniqueInteractionData(),
                                                                                 interaction.demographicData(),
                                                                                 sourceIdUid)))
            .build();
      return new InsertInteractionResult(DgraphClient.getInstance().doMutateTransaction(mutation), sourceIdUid);
   }

   private static String cloneGoldenRecordFromInteraction(
         final CustomDemographicData interaction,
         final String interactionUID,
         final String sourceUID,
         final float score,
         final CustomUniqueGoldenRecordData customUniqueGoldenRecordData) {
      final var command = CustomDgraphMutations.createLinkedGoldenRecordTriple(customUniqueGoldenRecordData,
                                                                               interaction,
                                                                               interactionUID,
                                                                               sourceUID,
                                                                               score);
      final DgraphProto.Mutation mutation = DgraphProto.Mutation.newBuilder()
                                                                .setSetNquads(ByteString.copyFromUtf8(command))
                                                                .build();
      return DgraphClient.getInstance().doMutateTransaction(mutation);
   }

   private static void deleteGoldenRecord(final String goldenId) {
      final var mutation = DgraphProto.Mutation.newBuilder()
                                               .setDelNquads(ByteString.copyFromUtf8(
                                                     String.format("""
                                                                    <%s> * *  .
                                                                   """,
                                                                   goldenId)))
                                               .build();
      DgraphClient.getInstance().doMutateTransaction(mutation);
   }

   static LinkInfo addNewDGraphInteraction(final Interaction interaction) {
      final var result = insertInteraction(interaction);
      if (result.interactionUID == null) {
         LOGGER.error("Failed to insert interaction");
         return null;
      }
      final var grUID = cloneGoldenRecordFromInteraction(interaction.demographicData(),
                                                         result.interactionUID,
                                                         result.sourceUID,
                                                         1.0F,
                                                         new CustomUniqueGoldenRecordData(true,
                                                                                          interaction.uniqueInteractionData()
                                                                                                     .auxId()));
      if (grUID == null) {
         LOGGER.error("Failed to insert golden record");
         return null;
      }
      return new LinkInfo(grUID, result.interactionUID, 1.0F);
   }

   static String camelToSnake(final String str) {
      return str.replaceAll("([A-Z]+)", "\\_$1").toLowerCase();
   }

   static boolean updateGoldenRecordField(
         final String goldenId,
         final String fieldName,
         final String val) {
      String predicate = "GoldenRecord." + camelToSnake(fieldName);
      return updateGoldenRecordPredicate(goldenId, predicate, val);
   }

   static boolean updateGoldenRecordField(
         final String goldenId,
         final String fieldName,
         final Boolean val) {
      String predicate = "GoldenRecord." + camelToSnake(fieldName);
      return updateGoldenRecordPredicate(goldenId, predicate, val);
   }

   static boolean updateGoldenRecordField(
         final String goldenId,
         final String fieldName,
         final Double val) {
      String predicate = "GoldenRecord." + camelToSnake(fieldName);
      return updateGoldenRecordPredicate(goldenId, predicate, val);
   }

   static boolean updateGoldenRecordField(
         final String goldenId,
         final String fieldName,
         final Long val) {
      String predicate = "GoldenRecord." + camelToSnake(fieldName);
      return updateGoldenRecordPredicate(goldenId, predicate, val);
   }

   static Either<MpiGeneralError, LinkInfo> linkToNewGoldenRecord(
         final String currentGoldenId,
         final String interactionId,
         final float score) {

      final var goldenUidInteractionUidList = DgraphQueries.findExpandedGoldenIds(currentGoldenId);
      if (goldenUidInteractionUidList.isEmpty() || !goldenUidInteractionUidList.contains(interactionId)) {
         return Either.left(
               new MpiServiceError.GoldenIdInteractionConflictError("Interaction not linked to GoldenRecord",
                                                                    currentGoldenId,
                                                                    interactionId));
      }
      final var count = goldenUidInteractionUidList.size();

      final var interaction = DgraphQueries.findInteraction(interactionId);
      if (interaction == null) {
         LOGGER.warn("interaction {} not found", interactionId);
         return Either.left(new MpiServiceError.InteractionIdDoesNotExistError("Interaction not found", interactionId));
      }
      final var grec = DgraphQueries.findDgraphGoldenRecord(currentGoldenId);
      if (grec == null) {
         return Either.left(new MpiServiceError.GoldenIdDoesNotExistError("Golden Record not found", currentGoldenId));
      }
      if (!deletePredicate(currentGoldenId, CustomDgraphConstants.PREDICATE_GOLDEN_RECORD_INTERACTIONS, interactionId)) {
         return Either.left(new MpiServiceError.DeletePredicateError(interactionId,
                                                                     CustomDgraphConstants.PREDICATE_GOLDEN_RECORD_INTERACTIONS));
      }
      if (count == 1) {
         deleteGoldenRecord(currentGoldenId);
      }
      final var newGoldenID = cloneGoldenRecordFromInteraction(
            interaction.demographicData(), interaction.interactionId(),
            interaction.sourceId().uid(),
            score, new CustomUniqueGoldenRecordData(true, interaction.uniqueInteractionData().auxId()));
      return Either.right(new LinkInfo(newGoldenID, interactionId, score));
   }

   static Either<MpiGeneralError, LinkInfo> updateLink(
         final String goldenId,
         final String newGoldenId,
         final String interactionId,
         final float score) {

      final var goldenUidInteractionUidList = DgraphQueries.findExpandedGoldenIds(goldenId);
      if (goldenUidInteractionUidList.isEmpty() || !goldenUidInteractionUidList.contains(interactionId)) {
         return Either.left(
               new MpiServiceError.GoldenIdInteractionConflictError("Interaction not linked to GoldenRecord", goldenId,
                                                                    interactionId));
      }

      final var count = DgraphQueries.countGoldenRecordEntities(goldenId);
      deletePredicate(goldenId, "GoldenRecord.interactions", interactionId);
      if (count == 1) {
         deleteGoldenRecord(goldenId);
      }

      final var scoreList = new ArrayList<DgraphPairWithScore>();
      scoreList.add(new DgraphPairWithScore(newGoldenId, interactionId, score));
      addScoreFacets(scoreList);
      return Either.right(new LinkInfo(newGoldenId, interactionId, score));
   }

   static LinkInfo linkDGraphInteraction(
         final Interaction interaction,
         final LibMPIClientInterface.GoldenIdScore goldenIdScore) {
      final var result = insertInteraction(interaction);

      if (result.interactionUID == null) {
         LOGGER.error("Failed to insert dgraphInteraction");
         return null;
      }
      final List<DgraphPairWithScore> interactionScoreList = new ArrayList<>();
      interactionScoreList.add(new DgraphPairWithScore(goldenIdScore.goldenId(), result.interactionUID, goldenIdScore.score()));
      addScoreFacets(interactionScoreList);
      addSourceId(interactionScoreList.get(0).goldenUID(), result.sourceUID);
      final var grUID = interactionScoreList.get(0).goldenUID();
      final var theScore = interactionScoreList.get(0).score();
      return new LinkInfo(grUID, result.interactionUID, theScore);
   }

   static Option<MpiGeneralError> createSchema() {
      final var schema = CustomDgraphConstants.MUTATION_CREATE_SOURCE_ID_TYPE
                         + CustomDgraphConstants.MUTATION_CREATE_GOLDEN_RECORD_TYPE
                         + CustomDgraphConstants.MUTATION_CREATE_INTERACTION_TYPE
                         + CustomDgraphConstants.MUTATION_CREATE_SOURCE_ID_FIELDS
                         + CustomDgraphConstants.MUTATION_CREATE_GOLDEN_RECORD_FIELDS
                         + CustomDgraphConstants.MUTATION_CREATE_INTERACTION_FIELDS;
      try {
         final DgraphProto.Operation operation = DgraphProto.Operation.newBuilder().setSchema(schema).build();
         DgraphClient.getInstance().alter(operation);
         return Option.none();
      } catch (RuntimeException ex) {
         LOGGER.warn("{}", schema);
         LOGGER.error(ex.getLocalizedMessage(), ex);
         return Option.of(new MpiServiceError.GeneralError("Create Schema Error"));
      }
   }

   public static boolean setScore(
         final String interactionUid,
         final String goldenRecordUid,
         final float score) {
      final var mutation = DgraphProto.Mutation.newBuilder()
                                               .setSetNquads(ByteString.copyFromUtf8(String.format(
                                                     "<%s> <GoldenRecord.interactions> <%s> (score=%f) .%n",
                                                     goldenRecordUid,
                                                     interactionUid,
                                                     score)))
                                               .build();
      final var result = DgraphClient.getInstance().doMutateTransaction(mutation);
      return StringUtil.isNullOrEmpty(result);
   }

   private record InsertInteractionResult(
         String interactionUID,
         String sourceUID) {
   }

}
