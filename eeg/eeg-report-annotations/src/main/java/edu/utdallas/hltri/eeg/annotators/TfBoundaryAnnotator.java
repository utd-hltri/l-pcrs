package edu.utdallas.hltri.eeg.annotators;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import edu.utdallas.hltri.conf.Config;
import edu.utdallas.hltri.eeg.TensorflowUtils;
import edu.utdallas.hltri.eeg.annotation.EegActivity;
import edu.utdallas.hltri.ml.label.IoLabel;
import edu.utdallas.hltri.scribe.annotators.Annotator;
import edu.utdallas.hltri.scribe.text.BaseDocument;
import edu.utdallas.hltri.scribe.text.Document;
import edu.utdallas.hltri.scribe.text.annotation.Event;
import edu.utdallas.hltri.scribe.text.annotation.Sentence;
import edu.utdallas.hltri.scribe.text.annotation.Token;
import edu.utdallas.hltri.ml.classify.IoSequenceChunker;
import edu.utdallas.hltri.util.IntIdentifier;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.HttpClients;

import java.io.*;
import java.util.*;

/**
 * Created by rmm120030 on 4/24/17.
 */
public class TfBoundaryAnnotator<D extends BaseDocument> implements Annotator<D> {

  private final String annset;
  private final HttpClient client;
  private final HttpPost post;
  private final IntIdentifier<String> featureMap;

  public static <D extends BaseDocument> TfBoundaryAnnotator<D> loadFromConfig(String annset) {
    final Config conf = Config.load("eeg");
    return new TfBoundaryAnnotator<>( conf.getString("nn.boundary.url"),
        conf.getString("nn.boundary.featuremap"), annset);
  }

  public TfBoundaryAnnotator(String url, String featureMapFile, String annset) {
    this.client = HttpClients.createDefault();
    this.post = new HttpPost(url);
    this.featureMap = IntIdentifier.fromFile(featureMapFile).lock();
    this.annset = annset;
  }

  private TfBoundaryAnnotator(TfBoundaryAnnotator.Builder<D> builder) {
    this.client = HttpClients.createDefault();
    this.post = new HttpPost(builder.url);
    this.featureMap = IntIdentifier.fromFile(builder.featureMapFile).lock();
    this.annset = builder.annset;
  }

  public static class Builder<D extends BaseDocument> extends Annotator.Builder<D,TfBoundaryAnnotator.Builder<D>> {
    private String url, featureMapFile, annset;

    @Override
    protected TfBoundaryAnnotator.Builder<D> self() {
      return this;
    }

    @Override
    public TfBoundaryAnnotator<D> build() {
      return new TfBoundaryAnnotator<>(this);
    }
  }

  @Override
  public <B extends D> void annotate(Document<B> document) {
    final List<List<String>> vectors = TensorflowUtils.generateUnlabeledBoundaryVectors(
        Collections.singletonList(document), featureMap);
    final Map<String, Token> id2Token = new HashMap<>();
    final List<String> ids = new ArrayList<>();
    for (Sentence sentence : document.get("opennlp", Sentence.TYPE)) {
      for (Token token : sentence.getContained("genia", Token.TYPE)) {
        final String tid = "#" + document.getId() + "|" + sentence.getId() + "|" + token.getId();
        id2Token.put(tid, token);
        ids.add(tid);
//        System.out.println("Token " + tid);
      }
//      System.out.println("-");
    }
    assert vectors.stream().mapToInt(List::size).sum() == ids.size();
    // ᕙ༼ຈل͜ຈ༽ᕗ
    final StringBuilder bodyBuilder = new StringBuilder();
    for (List<String> sequence : vectors) {
      for (String vec : sequence) {
        bodyBuilder.append(vec).append("\n");
      }
      bodyBuilder.append("\n");
    }
    // Request parameters and other properties.
    try {
      post.setEntity(new StringEntity(bodyBuilder.toString()));

      //Execute and get the response.
      HttpResponse response = client.execute(post);
      HttpEntity entity = response.getEntity();

      final Map<String, IoLabel> activityLabels = new HashMap<>(), eventLabels = new HashMap<>();
      if (entity != null) {
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(entity.getContent(), "UTF-8"))) {
          String line;
          while((line = reader.readLine())!=null) {
            if (line.length() > 0) {
              final String[] arr = line.split(" ");
//              System.out.println("Found id: " + arr[0]);
              activityLabels.put(arr[0], IoLabel.valueOf(arr[1]));
              eventLabels.put(arr[0], IoLabel.valueOf(arr[2]));
            }
          }
        }
      }

      final IoSequenceChunker<Token> activityCombiner = new IoSequenceChunker<Token>() {
        @Override
        public void combine(int start, int end, String label) {
          EegActivity.TYPE.create(document, annset, start, end);
        }

        @Override
        public int getStart(Token token) {
          return token.get(Token.StartOffset).intValue();
        }

        @Override
        public int getEnd(Token token) {
          return token.get(Token.EndOffset).intValue();
        }
      };
      final IoSequenceChunker<Token> eventCombiner = new IoSequenceChunker<Token>() {
        @Override
        public void combine(int start, int end, String label) {
          Event.TYPE.create(document, annset, start, end);
        }

        @Override
        public int getStart(Token token) {
          return token.get(Token.StartOffset).intValue();
        }

        @Override
        public int getEnd(Token token) {
          return token.get(Token.EndOffset).intValue();
        }
      };

      assert activityLabels.size() == ids.size() : "sent: " + ids.size() + ", received: " + activityLabels.size() +
          ". Sent - received: " + setDif(Sets.newHashSet(ids), activityLabels.keySet());
      for (String id : ids) {
        final Token token = id2Token.get(id);
//        System.out.println("Processing id: " + id);
        activityCombiner.processToken(token, activityLabels.get(id));
        eventCombiner.processToken(token, eventLabels.get(id));
      }
      activityCombiner.close();
      eventCombiner.close();
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  private static Set<String> setDif(Set<String> s1, Set<String> s2) {
    s1.removeAll(s2);
    return s1;
  }
}
