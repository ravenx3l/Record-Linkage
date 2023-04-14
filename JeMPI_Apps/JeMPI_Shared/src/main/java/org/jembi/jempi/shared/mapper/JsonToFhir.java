package org.jembi.jempi.shared.mapper;

import ca.uhn.fhir.context.FhirContext;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.hl7.fhir.r4.model.*;
import org.jembi.jempi.shared.models.*;
import org.jembi.jempi.shared.utils.JsonFieldsConfig;
import org.json.simple.JSONObject;
import java.lang.reflect.Field;
import java.util.List;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;

public final class JsonToFhir {

    private static JsonFieldsConfig jsonFieldsConfig = new JsonFieldsConfig();

    private JsonToFhir() throws Exception {
    }

    private static final Logger LOGGER = LogManager.getLogger(JsonToFhir.class);
    private static String getFhirPath(final String fieldName) {
        try {
            for (int i = 0; i < jsonFieldsConfig.fields.size(); i++) {
                JSONObject field = (JSONObject) jsonFieldsConfig.fields.get(i);
                if (fieldName.equalsIgnoreCase((String) field.get("fieldName"))) {
                    return (String) field.get("fhirPath");
                }
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        return null;
    }

    private static void processField(final Patient patient, final String fieldValue, final String fhirPath) {
        switch (fhirPath) {
            case "Patient.identifier" -> {
                Identifier identifier = new Identifier();
                identifier.setValue(fieldValue);
                patient.addIdentifier(identifier);
            }
            case "name.given" -> {
                HumanName name = new HumanName();
                name.addGiven(fieldValue);
                patient.addName(name);
            }
            case "name.family" -> {
                HumanName familyName = new HumanName();
                familyName.setFamily(fieldValue);
                patient.addName(familyName);
            }
            case "address.city" -> {
                Address address = new Address();
                address.setCity(fieldValue);
                patient.addAddress(address);
            }
            case "birthDate" -> {
                try {
                    LocalDate date = LocalDate.parse(fieldValue, DateTimeFormatter.BASIC_ISO_DATE);
                    String fhirDate = date.format(DateTimeFormatter.ISO_LOCAL_DATE);
                    DateType birthDate = new DateType(fhirDate);
                    patient.setBirthDate(birthDate.getValue());
                } catch (DateTimeParseException e) {
                    LOGGER.error("Failed to parse birth date: " + fieldValue, e);
                }
            }
            default -> {
                // to be implemented
            }
        }
    }

    private static Patient mapToPatientFhir(
            final String resourceId, final CustomDemographicData demographicData, final List<SourceId> sourceId) {
        Patient patient = new Patient();
            try {
                Identifier identifier = new Identifier();
                identifier.setValue(resourceId);
                patient.addIdentifier(identifier);
                for (Field demoField : CustomDemographicData.class.getDeclaredFields()) {
                    demoField.setAccessible(true);
                    String demoFieldName = demoField.getName();
                    String fieldValue = (String) demoField.get(demographicData);
                    if (fieldValue != null) {
                        String fhirPath = getFhirPath(demoFieldName);
                        if (fhirPath != null) {
                            processField(patient, fieldValue, fhirPath);
                        }
                    }
                }
                for (SourceId source: sourceId) {
                    Organization organization = new Organization();
                    organization.setId(source.uid());
                    patient.getManagingOrganization().setResource(organization);
                }

            } catch (IllegalAccessException e) {
                e.printStackTrace();
                LOGGER.debug(e);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        return patient;

    }

    public static String mapPatientRecordToFhirFormat(
            final PatientRecord patientRecord, final List<GoldenRecordWithScore> goldenRecordWithScoreList) {
        Patient patient = mapToPatientFhir(
                patientRecord.patientId(), patientRecord.demographicData(), List.of(patientRecord.sourceId()));
        patient.addLink()
                .setOther(new Reference(String.format("Patient/%s", goldenRecordWithScoreList.get(0)
                        .goldenRecord()
                        .goldenId())))
                .setType(Patient.LinkType.REFER);
        FhirContext ctx = FhirContext.forR4();
        return ctx.newJsonParser().setPrettyPrint(true).encodeResourceToString(patient);
    }

    public static String mapGoldenRecordToFhirFormat(
            final GoldenRecord goldenRecord, final List<PatientRecordWithScore> patientRecordWithScoreList) {
        Patient patient = mapToPatientFhir(
                goldenRecord.goldenId(), goldenRecord.demographicData(), goldenRecord.sourceId());
        for (PatientRecordWithScore currPatient : patientRecordWithScoreList) {
            patient.addLink()
                    .setOther(new Reference(String.format("Patient/%s", currPatient.patientRecord().patientId())))
                    .setType(Patient.LinkType.SEEALSO);
        }
        FhirContext ctx = FhirContext.forR4();
        return ctx.newJsonParser().setPrettyPrint(true).encodeResourceToString(patient);
    }
}