package org.jembi.jempi.linker;

import akka.actor.typed.ActorRef;
import akka.actor.typed.ActorSystem;
import akka.actor.typed.javadsl.AskPattern;
import org.jembi.jempi.linker.backend.BackEnd;
import org.jembi.jempi.shared.models.ApiModels;
import org.jembi.jempi.shared.models.InteractionEnvelop;
import org.jembi.jempi.shared.models.LinkInteractionSyncBody;
import org.jembi.jempi.shared.models.LinkInteractionToGidSyncBody;

import java.util.concurrent.CompletionStage;

final class Ask {

   private Ask() {
   }

   static CompletionStage<BackEnd.SyncLinkInteractionResponse> postLinkInteraction(
         final ActorSystem<Void> actorSystem,
         final ActorRef<BackEnd.Request> backEnd,
         final LinkInteractionSyncBody body) {
      CompletionStage<BackEnd.SyncLinkInteractionResponse> stage = AskPattern.ask(backEnd,
                                                                                  replyTo -> new BackEnd.SyncLinkInteractionRequest(
                                                                                        body,
                                                                                        replyTo),
                                                                                  java.time.Duration.ofSeconds(11),
                                                                                  actorSystem.scheduler());
      return stage.thenApply(response -> response);
   }

   static CompletionStage<BackEnd.AsyncLinkInteractionResponse> linkInteraction(
         final ActorSystem<Void> actorSystem,
         final ActorRef<BackEnd.Request> backEnd,
         final String key,
         final InteractionEnvelop batchInteraction) {
      return AskPattern.ask(backEnd,
                            replyTo -> new BackEnd.AsyncLinkInteractionRequest(replyTo, key, batchInteraction),
                            java.time.Duration.ofSeconds(60),
                            actorSystem.scheduler());
   }

   static CompletionStage<BackEnd.FindCandidatesWithScoreResponse> findCandidates(
         final ActorSystem<Void> actorSystem,
         final ActorRef<BackEnd.Request> backEnd,
         final String iid) {
      CompletionStage<BackEnd.FindCandidatesWithScoreResponse> stage = AskPattern
            .ask(backEnd,
                 replyTo -> new BackEnd.FindCandidatesWithScoreRequest(replyTo, iid),
                 java.time.Duration.ofSeconds(5),
                 actorSystem.scheduler());
      return stage.thenApply(response -> response);
   }

   static CompletionStage<BackEnd.SyncLinkInteractionToGidResponse> postLinkPatientToGid(
         final ActorSystem<Void> actorSystem,
         final ActorRef<BackEnd.Request> backEnd,
         final LinkInteractionToGidSyncBody body) {
      CompletionStage<BackEnd.SyncLinkInteractionToGidResponse> stage = AskPattern.ask(backEnd,
                                                                                       replyTo -> new BackEnd.SyncLinkInteractionToGidRequest(
                                                                                             body,
                                                                                             replyTo),
                                                                                       java.time.Duration.ofSeconds(11),
                                                                                       actorSystem.scheduler());
      return stage.thenApply(response -> response);
   }

   static CompletionStage<BackEnd.CalculateScoresResponse> postCalculateScores(
         final ActorSystem<Void> actorSystem,
         final ActorRef<BackEnd.Request> backEnd,
         final ApiModels.ApiCalculateScoresRequest body) {
      CompletionStage<BackEnd.CalculateScoresResponse> stage = AskPattern.ask(
            backEnd,
            replyTo -> new BackEnd.CalculateScoresRequest(body, replyTo),
            java.time.Duration.ofSeconds(11),
            actorSystem.scheduler());
      return stage.thenApply(response -> response);
   }

   static CompletionStage<BackEnd.EventGetMURsp> getMU(
         final ActorSystem<Void> actorSystem,
         final ActorRef<BackEnd.Request> backEnd) {
      CompletionStage<BackEnd.EventGetMURsp> stage =
            AskPattern.ask(backEnd, BackEnd.EventGetMUReq::new, java.time.Duration.ofSeconds(11), actorSystem.scheduler());
      return stage.thenApply(response -> response);
   }

}