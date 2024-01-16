package org.jembi.jempi.shared.libs.linking;

import org.apache.commons.lang3.StringUtils;
import org.jembi.jempi.shared.models.CustomDemographicData;

public final class CustomLinkerDeterministic {

   static final boolean DETERMINISTIC_DO_LINKING = true;
   static final boolean DETERMINISTIC_DO_VALIDATING = false;
   static final boolean DETERMINISTIC_DO_MATCHING = false;

   private CustomLinkerDeterministic() {
   }

   private static boolean isMatch(
         final String left,
         final String right) {
      return StringUtils.isNotBlank(left) && StringUtils.equals(left, right);
   }

   static boolean canApplyLinking(
         final CustomDemographicData interaction) {
      return CustomLinkerProbabilistic.PROBABILISTIC_DO_LINKING
             || StringUtils.isNotBlank(interaction.nationalId)
             || StringUtils.isNotBlank(interaction.givenName)
             && StringUtils.isNotBlank(interaction.familyName)
             && StringUtils.isNotBlank(interaction.phoneNumber);
   }

   public static boolean linkDeterministicMatch(
         final CustomDemographicData goldenRecord,
         final CustomDemographicData interaction) {
      final var nationalIdL = goldenRecord.nationalId;
      final var nationalIdR = interaction.nationalId;
      if (isMatch(nationalIdL, nationalIdR)) {
         return true;
      }
      final var givenNameL = goldenRecord.givenName;
      final var givenNameR = interaction.givenName;
      final var familyNameL = goldenRecord.familyName;
      final var familyNameR = interaction.familyName;
      final var phoneNumberL = goldenRecord.phoneNumber;
      final var phoneNumberR = interaction.phoneNumber;
      return (isMatch(givenNameL, givenNameR) && isMatch(familyNameL, familyNameR) && isMatch(phoneNumberL, phoneNumberR));
   }

   public static boolean validateDeterministicMatch(
         final CustomDemographicData goldenRecord,
         final CustomDemographicData interaction) {
      return false;
   }

   public static boolean matchNotificationDeterministicMatch(
         final CustomDemographicData goldenRecord,
         final CustomDemographicData interaction) {
      return false;
   }

}
