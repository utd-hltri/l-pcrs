//package edu.utdallas.hltri.mercury.scripts;
//
//import java.nio.file.Path;
//import java.nio.file.Paths;
//
//import edu.utdallas.hltri.metamap.MetamapOutput;
//import edu.utdallas.hltri.scribe.io.JsonCorpus;
//
///**
// * Created by guy with a purse on 11/4/16.
// */
//public class JamiaMetamapAttacher {
//  public static void main(String... args) {
//    final JsonCorpus<JamiaVectorizer2.TrecMedTopic> topics = JsonCorpus.<JamiaVectorizer2.TrecMedTopic>at(Paths.get(args[0])).build();
//    final Path path = Paths.get(args[1]);
//    MetamapOutput.attachMetamapConcepts(path.toAbsolutePath().toString(), topics);
//  }
//}
