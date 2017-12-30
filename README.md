# Repository for the Learning Patient Cohort Retrieval (L-PCR) System

This project was written in Java, version 8.

Detailed instructions are coming soon!

In the meantime, most of the magic happens in [CohortL2rProcessor.java](inquire/inquire-med/src/main/java/edu/utdallas/hltri/inquire/l2r/CohortL2rProcessor.java).

The feature extraction code for the TRECMed experiments is available [here](trecmed/src/main/java/edu/utdallas/hlt/trecmed/scripts/JamiaVectorizer2.java), and for EEGs it is available [here](mercury/mercury-core/src/main/java/edu/utdallas/hltri/mercury/jamia/scripts/JamiaVectorizer2.java).

The random forest classifiers were trained using [RankLib](https://sourceforge.net/p/lemur/wiki/RankLib/), part of the [Lemur project](https://www.lemurproject.org/indri/).




