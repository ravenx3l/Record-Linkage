package org.jembi.jempi.libmpi.dgraph;

import io.dgraph.DgraphProto;
import io.vavr.control.Either;
import io.vavr.control.Option;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jembi.jempi.libmpi.*;
import org.jembi.jempi.shared.models.CustomDemographicData;
import org.jembi.jempi.shared.models.CustomGoldenRecord;
import org.jembi.jempi.shared.models.CustomPatient;
import org.jembi.jempi.shared.models.LinkInfo;
import org.jembi.jempi.shared.utils.LibMPIPaginatedResultSet;
import org.jembi.jempi.shared.utils.SimpleSearchRequestPayload;

import java.util.List;

import static io.dgraph.DgraphProto.Operation.DropOp.DATA;

public class LibDgraph implements LibMPIClientInterface {

   private static final Logger LOGGER = LogManager.getLogger(LibDgraph.class);

   public LibDgraph(
         final String[] host,
         final int[] port) {
      LOGGER.info("{}", "LibDgraph Constructor");

      Client.getInstance().config(host, port);
   }

   /*
    * *******************************************************
    * QUERIES
    * *******************************************************
    *
    */

   public LibMPIPaginatedResultSet<MpiExpandedGoldenRecord> simpleSearchGoldenRecords(
         List<SimpleSearchRequestPayload.SearchParameter> params,
         Integer offset,
         Integer limit,
         String sortBy,
         Boolean sortAsc
                                                                                     ) {
      final var list = Queries.simpleSearchGoldenRecords(params, offset, limit, sortBy, sortAsc);
      if (list == null) {
         return null;
      }
      final var data = list.all().stream().map(CustomLibMPIExpandedGoldenRecord::toMpiExpandedGoldenRecord).toList();
      final var pagination = list.pagination().get(0);
      return new LibMPIPaginatedResultSet(data, pagination);
   }

   public LibMPIPaginatedResultSet<MpiExpandedGoldenRecord> customSearchGoldenRecords(
         List<SimpleSearchRequestPayload> params,
         Integer offset,
         Integer limit,
         String sortBy,
         Boolean sortAsc
                                                                                     ) {
      final var list = Queries.customSearchGoldenRecords(params, offset, limit, sortBy, sortAsc);
      if (list == null) {
         return null;
      }
      final var data = list.all().stream().map(CustomLibMPIExpandedGoldenRecord::toMpiExpandedGoldenRecord).toList();
      final var pagination = list.pagination().get(0);
      return new LibMPIPaginatedResultSet(data, pagination);
   }

   public LibMPIPaginatedResultSet<CustomPatient> simpleSearchPatientRecords(
         List<SimpleSearchRequestPayload.SearchParameter> params,
         Integer offset,
         Integer limit,
         String sortBy,
         Boolean sortAsc
                                                                            ) {
      final var list = Queries.simpleSearchPatientRecords(params, offset, limit, sortBy, sortAsc);
      if (list == null) {
         return null;
      }
      final var data = list.all().stream().map(CustomLibMPIDGraphPatient::toCustomPatient).toList();
      final var pagination = list.pagination().get(0);
      return new LibMPIPaginatedResultSet(data, pagination);
   }

   public LibMPIPaginatedResultSet<CustomPatient> customSearchPatientRecords(
         List<SimpleSearchRequestPayload> params,
         Integer offset,
         Integer limit,
         String sortBy,
         Boolean sortAsc
                                                                            ) {
      final var list = Queries.customSearchPatientRecords(params, offset, limit, sortBy, sortAsc);
      if (list == null) {
         return null;
      }
      final var data = list.all().stream().map(CustomLibMPIDGraphPatient::toCustomPatient).toList();
      final var pagination = list.pagination().get(0);
      return new LibMPIPaginatedResultSet(data, pagination);
   }

   public List<CustomGoldenRecord> getCandidates(
         final CustomDemographicData demographicData,
         final boolean applyDeterministicFilter) {
      final var candidates = CustomLibMPIQueries.getCandidates(demographicData, applyDeterministicFilter);
      return candidates.stream().map(CustomLibMPIGoldenRecord::toCustomGoldenRecord).toList();
   }

   public List<MpiExpandedPatient> getMpiExpandedPatients(final List<String> idList) {
      final var list = Queries.getExpandedPatients(idList);
      return list.stream().map(CustomLibMPIExpandedPatient::toMpiExpandedPatient).toList();
   }

   public List<MpiExpandedGoldenRecord> getMpiExpandedGoldenRecordList(final List<String> goldenIdList) {
      final var list = Queries.getExpandedGoldenRecordList(goldenIdList);
      return list.stream().map(CustomLibMPIExpandedGoldenRecord::toMpiExpandedGoldenRecord).toList();
   }

   public List<String> getGoldenIdListByPredicate(
         final String predicate,
         final String val) {
      return Queries.getGoldenIdListByPredicate(predicate, val);
   }

   public CustomGoldenRecord getGoldenRecordByUid(final String uid) {
      final var rec = Queries.getGoldenRecordByUid(uid);
      if (rec == null) {
         return null;
      }
      return rec.toCustomGoldenRecord();
   }

   public CustomPatient getPatient(final String uid) {
      return Queries.getDGraphPatient(uid);
   }

   public List<String> getGoldenIdList() {
      return Queries.getGoldenIdList();
   }

   public long countGoldenRecords() {
      return Queries.countGoldenRecords();
   }

   public long countPatients() {
      return Queries.countPatients();
   }

   /*
    * *******************************************************
    * MUTATIONS
    * *******************************************************
    */

   public boolean updateGoldenRecordField(
         final String uid,
         final String fieldName,
         final String val) {
      return Mutations.updateGoldenRecordField(uid, fieldName, val);
   }

   public Either<MpiGeneralError, LinkInfo> unLink(
         final String goldenUID,
         final String patientUID,
         final float score) {
      return Mutations.unLink(goldenUID, patientUID, score);
   }

   public Either<MpiGeneralError, LinkInfo> updateLink(
         final String goldenUID,
         final String newGoldenUID,
         final String patientUID,
         final float score) {
      return Mutations.updateLink(goldenUID, newGoldenUID, patientUID, score);
   }

   public LinkInfo createPatientAndLinkToExistingGoldenRecord(
         final CustomPatient patient,
         final GoldenUIDScore goldenUIDScore) {
      return Mutations.linkDGraphPatient(patient, goldenUIDScore);
   }

   public LinkInfo createPatientAndLinkToClonedGoldenRecord(
         final CustomPatient patient,
         float score) {
      return Mutations.addNewDGraphPatient(patient);
   }

   public void startTransaction() {
      Client.getInstance().startTransaction();
   }

   public void closeTransaction() {
      Client.getInstance().closeTransaction();
   }

   /*
    * *******************************************************
    * DATABASE
    * *******************************************************
    */

   public Option<MpiGeneralError> dropAll() {
      try {
         Client.getInstance().alter(DgraphProto.Operation.newBuilder().setDropAll(true).build());
         return Option.none();
      } catch (RuntimeException e) {
         LOGGER.error(e.getMessage(), e);
         return Option.of(new MpiServiceError.GeneralError("Drop All Error"));
      }
   }

   public Option<MpiGeneralError> dropAllData() {
      try {
         Client.getInstance().alter(DgraphProto.Operation.newBuilder().setDropOp(DATA).build());
         return Option.none();
      } catch (RuntimeException e) {
         LOGGER.error(e.getMessage());
         return Option.of(new MpiServiceError.GeneralError("Drop All Data Error"));
      }
   }

   public Option<MpiGeneralError> createSchema() {
      return Mutations.createSchema();
   }

}
